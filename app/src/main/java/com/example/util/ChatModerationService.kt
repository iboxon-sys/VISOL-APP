package com.example.util

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.UserRepository
import com.example.model.ChatMessage
import com.example.model.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

/**
 * 2-Bosqichli Chat Moderatsiyasi va O'zbekona Odob-Axloq Qoidalari (AI Hakam).
 * 
 * 1-Bosqich: Lokal o'zbekcha so'kinishlar va haqoratlar bazasi (Tezkor regex & lug'at).
 * 2-Bosqich: Gemini AI chuqur kontekstual tahlili (Mentalitet, pinhona haqorat, bulling, nojo'ya talablar).
 * 
 * Mojaroning oldini olish (3-shaxs sxemasi):
 * - Qoidabuzar yozgan xabar suhbatdoshga (2-shaxsga) mutlaqo bormaydi (zanjir uziladi).
 * - Qoidabuzarning o'ziga chat ichida 3-shaxs (AI Hakam) tomonidan maxfiy ogohlantirish chiqariladi.
 * - Shu zahotiyoq Adminning Telegram botiga dalil va boshqaruv tugmalari bilan xabar yuboriladi.
 */
object ChatModerationService {

    private const val TAG = "ChatModerationService"

    data class TextModerationResult(
        val isApproved: Boolean,
        val violationReason: String = "",
        val detectedRule: String = "",
        val aiHakamWarning: String = ""
    )

    // 1-BOSQICH: O'zbekona odob-axloqqa zid so'zlar va iboralar lug'ati (Lokal tezkor filtr)
    private val UZBEK_PROHIBITED_PATTERNS = listOf(
        // Jinsiy a'zolar va qo'pol anatomik so'zlar (Lotin va Kirill)
        Pattern.compile(".*(qo'?to[qg]|qoto[qg]|kot[oi]ng|ko'?t|chlen|vagina|pizda|xuy|huy|sikish|jinsiy\\s*a'?zo|jinsiy\\s*aloqa|yotaylik|qizliging).*"),
        // Onadan / Oiladan so'kishlar
        Pattern.compile(".*(onang|onangni|onangi|onangday|onasi|enag|enangi|enangni|otang|otangni|opang|opangni|singling|singlingni|ajdoding|avlod|pochang|kelin)\\s*(sik|siq|em|sike|siktir|it|eshak|jalab|haromi|foxisha|buzuq|ko'?t).*"),
        Pattern.compile(".*(onangdi|onangdayin|onangnisik|enagni|oneni|onasi|sikey|skay|ske).*"),
        // Ko'cha so'kinishlari va uyatsiz so'zlar
        Pattern.compile(".*(sikay|siktir|sikish|sikaman|sikmoq|sikesan|skaman|skay|sikishaylik|siktirgin|jalab|jalap|foxisha|faxisha|fahisha|haromi|haromzoda|xaromi|itvachcha|isqirt|dalbayob|dalbayop|dolboyob|dalbaeb|chmo|gandon|yeban|ebat|blyad|blyat|suka|naxuy|poshol).*"),
        // Ochiq-sochiq / nojo'ya intim talablar
        Pattern.compile(".*(yalang'?och|yalangoch|echin|yechin|ochiq\\s*rasm|ichki\\s*kiyim|to'?shak|toshak|aloqa\\s*qilaylik|intim|seks|sex|porno|tanamni|ko'?kraging|kokrak|dosh).*"),
        // Psixologik bosim / Tahdid / Bulling
        Pattern.compile(".*(o'?ldiraman|oldiraman|so'?yaman|soyaman|yo'?q\\s*qilaman|yoq\\s*qilaman|sharmanda\\s*qilaman|rasvo\\s*qilaman|uyingni\\s*yoqaman|tinch\\s*qo'?ymayman|shantaj|zahar\\s*ich|o'?zingni\\s*o'?ldir).*")
    )

    private val KEYWORD_BLACKLIST = setOf(
        "onangni", "onangdi", "onangnisik", "enagni", "enanga", "onasi", "qotoq", "qo'toq", "qotog", "qotog'im",
        "sik", "sikay", "sikaman", "sikish", "siktir", "sikesan", "jalab", "jalap", "foxisha", "fahisha", "haromi",
        "haromzoda", "itvachcha", "dalbayob", "dalbaeb", "gandon", "yeban", "aming", "am", "ami", "amcha", "amiga",
        "ko't", "kot", "koting", "ko'ting", "kotvachcha", "suka", "blyad", "blyat", "armanda", "naxuy", "poshol",
        "chmo", "sikey", "skay", "ske", "sikding", "so'kinding", "sukinding", "qanjiq", "padaringa", "maraz", "isqirt",
        "chlen", "pizda", "huy", "xuy", "jopa", "seks", "sex", "porno", "куток", "сик", "сикаман", "ам", "аминг",
        "жалаб", "гандон", "далбаеб", "далбаёб", "пизда", "хуй", "сука", "блядь"
    )

    // Substring sifatida ham tekshiriladigan uzunroq xavfli so'z o'zaklari
    private val STEM_BLACKLIST = listOf(
        "qotoq", "qo'toq", "sikay", "sikish", "sikaman", "siktir", "dalbayob", "dalbaeb", "gandon",
        "itvachcha", "haromzoda", "haromi", "onangni", "enagni", "onangdi", "chlen", "pizda", "poshol", "naxuy",
        "куток", "далбаёб", "гандон", "жалаб"
    )

    /**
     * Matnni 2 bosqichda tekshirish:
     * 1. Lokal o'zbekcha lug'at va Regex
     * 2. Gemini AI chuqur tahlili
     */
    suspend fun inspectChatMessage(
        context: Context,
        senderUser: UserProfile,
        receiverCandidate: UserProfile,
        messageText: String
    ): TextModerationResult = withContext(Dispatchers.Default) {
        val trimmed = messageText.trim()
        if (trimmed.isBlank()) {
            return@withContext TextModerationResult(isApproved = true)
        }

        val normalized = trimmed.lowercase(Locale.ROOT)
            .replace("@", "a")
            .replace("0", "o")
            .replace("1", "i")
            .replace("3", "e")
            .replace("$", "s")
            .replace("!", "i")

        // 1-BOSQICH: Tezkor lokal tekshiruv (Pattern va Lug'at)
        for (pattern in UZBEK_PROHIBITED_PATTERNS) {
            if (pattern.matcher(normalized).find() || pattern.matcher(trimmed).find()) {
                val warning = "⚖️ [AI Hakam Ogohlantirishi]: Xabaringizda o'zbekona odob-axloq me'yorlariga zid nomaqbul so'zlar aniqlandi. Mojaroning oldini olish maqsadida ushbu xabar suhbatdoshingizga yetkazilmadi va bloklandi. Iltimos, xushmuomala bo'ling!"
                val result = TextModerationResult(
                    isApproved = false,
                    violationReason = "O'zbekona odob-axloqqa zid so'z/haqorat (1-bosqich tezkor filtr)",
                    detectedRule = "Haqorat / Nomaqbul so'zlar",
                    aiHakamWarning = warning
                )
                sendChatAlertToTelegram(context, senderUser, receiverCandidate, trimmed, result.violationReason)
                return@withContext result
            }
        }

        val words = normalized.split(Regex("[\\s.,!?;:()\\-_/]+"))
        val collapsed = normalized.filter { it.isLetter() }

        val isProhibitedWord = words.any { KEYWORD_BLACKLIST.contains(it) } ||
                STEM_BLACKLIST.any { normalized.contains(it) || collapsed.contains(it) }

        if (isProhibitedWord) {
            val warning = "⚖️ [AI Hakam Ogohlantirishi]: Yozgan xabaringizda nojo'ya yoki haqoratli so'zlar aniqlandi. Suhbat madaniyatiga rioya qiling. Xabaringiz suhbatdoshga bormadi."
            val result = TextModerationResult(
                isApproved = false,
                violationReason = "O'zbekona so'kinish yoki nomaqbul so'z aniqlandi",
                detectedRule = "So'kinish / Haqorat",
                aiHakamWarning = warning
            )
            sendChatAlertToTelegram(context, senderUser, receiverCandidate, trimmed, result.violationReason)
            return@withContext result
        }

        // 2-BOSQICH: Gemini AI chuqur tahlili (Contextual AI Hakam)
        val geminiAssessment = checkMessageWithGeminiAI(trimmed)
        if (!geminiAssessment.isApproved) {
            sendChatAlertToTelegram(context, senderUser, receiverCandidate, trimmed, geminiAssessment.violationReason)
            return@withContext geminiAssessment
        }

        TextModerationResult(isApproved = true)
    }

    /**
     * Gemini AI orqali kontekstual, pinhona haqorat, tahdid va ochiq-sochiqlikni tahlil qilish
     */
    private suspend fun checkMessageWithGeminiAI(messageText: String): TextModerationResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            field.get(null) as? String ?: ""
        } catch (e: Throwable) {
            System.getenv("GEMINI_API_KEY") ?: ""
        }

        if (apiKey.isBlank()) {
            return@withContext TextModerationResult(isApproved = true)
        }

        try {
            val prompt = """
                Siz "Visol.uz" (O'zbekiston oilaviy tanishuv va nikoh ilovasi) uchun mas'ul bo'lgan xolis "AI Hakam"siz.
                Quyidagi chat xabarini o'zbek milliy mentaliteti, odob-axloqi va tanishuv qoidalari nuqtai nazaridan tekshiring.

                Tekshirish mezonlari:
                1. Onadan, oiladan yoki shaxsiyatga tegadigan o'zbekcha haqoratlar.
                2. Intim, behayo, jinsiy a'zolarni tilga olish (qo'toq, am, ko't, chlen, xuy, pizda va boshqalar), ochiq-sochiq rasm talab qilish yoki shahvoniy takliflar.
                3. Psixologik tazyiq, qo'rqitish, do'q-po'pisa yoki tahdid (bulling).
                4. Ochiq ko'cha so'kinishlari (ruscha/o'zbekcha).
                Agarda xabarda jinsiy a'zo yoki so'kinish bo'lsa, QAT'IYAN is_clean: false bering!

                Xabar matni: "$messageText"

                Faqat quyidagi JSON formatida qat'iy javob bering (boshqa hech narsa yozmang):
                {
                   "is_clean": true yoki false,
                   "reason": "O'zbek tilida qisqa sababi",
                   "category": "Odob qoidasi / Haqorat / Intim talab / Tahdid / Toza"
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            val textPart = JSONObject().apply {
                                put("text", prompt)
                            }
                            put(textPart)
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.1)
                    put("response_mime_type", "application/json")
                }
                put("generationConfig", genConfig)
            }

            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; utf-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 4000
                readTimeout = 4000
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            if (conn.responseCode in 200..299) {
                val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).readText()
                val respJson = JSONObject(responseStr)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    val rawText = parts.getJSONObject(0).getString("text")
                    val cleanJsonStr = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val resultJson = JSONObject(cleanJsonStr)

                    val isClean = resultJson.optBoolean("is_clean", true)
                    val reason = resultJson.optString("reason", "Odob-axloq me'yorlari buzilishi")
                    val category = resultJson.optString("category", "Odob qoidasi")

                    if (!isClean) {
                        val warning = "⚖️ [AI Hakam Ogohlantirishi]: Yozgan xabaringizda o'zbekona odob-axloq qoidalariga zid holat ($reason) aniqlandi. Mojaroning oldini olish uchun xabar suhbatdoshga yetkazilmadi. Xushmuomala bo'ling!"
                        return@withContext TextModerationResult(
                            isApproved = false,
                            violationReason = reason,
                            detectedRule = category,
                            aiHakamWarning = warning
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gemini API check skipped or timeout: ${e.message}")
        }

        TextModerationResult(isApproved = true)
    }

    /**
     * Adminning Telegram Botiga Chat Qoidabuzarligi bo'yicha Dalilli Xabar Jo'natish
     */
    private suspend fun sendChatAlertToTelegram(
        context: Context,
        sender: UserProfile,
        receiver: UserProfile,
        messageText: String,
        reason: String
    ) = withContext(Dispatchers.IO) {
        try {
            val botToken = TelegramModerationBotService.getBotToken(context)
            val chatId = TelegramModerationBotService.getAdminChatId(context)

            val timeStr = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date())

            val alertText = buildString {
                append("🚨 <b>CHAT MODERATSIYASI — QOIDABUZARLIK TO'XTATILDI!</b> 🚨\n\n")
                append("👤 <b>Qoidabuzar (1-shaxs):</b> ${sender.fullName} (${sender.username})\n")
                append("🆔 <b>Sender ID:</b> <code>${sender.id}</code>\n")
                append("👥 <b>Jabrlanuvchi (2-shaxs):</b> ${receiver.fullName} (${receiver.username})\n")
                append("🛡 <b>AI Hakam Harakati:</b> <i>Zanjir uzildi (xabar suhbatdoshga bormadi)</i>\n\n")
                append("💬 <b>Haqoratli xabar matni (Dalil):</b>\n")
                append("<blockquote>${messageText}</blockquote>\n\n")
                append("⚠️ <b>Aniqlangan qoidabuzarlik:</b> ${reason}\n")
                append("⏱ <b>Vaqt:</b> ${timeStr}\n\n")
                append("📌 <i>Admin uchun quyidagi tugmalar orqali qoidabuzarga chora ko'rish mumkin:</i>")
            }

            val keyboardJson = JSONObject().apply {
                val inlineKeyboard = JSONArray().apply {
                    val row1 = JSONArray().apply {
                        val btnApprove = JSONObject().apply {
                            put("text", "✅ Ruxsat berish (Ogohlantirishni bekor qilish)")
                            put("callback_data", "chat_approve_${sender.id}")
                        }
                        put(btnApprove)
                    }
                    val row2 = JSONArray().apply {
                        val btnReject = JSONObject().apply {
                            put("text", "❌ Rad etish va Foydalanuvchini Ban qilish")
                            put("callback_data", "chat_ban_${sender.id}")
                        }
                        put(btnReject)
                    }
                    put(row1)
                    put(row2)
                }
                put("inline_keyboard", inlineKeyboard)
            }

            val requestBody = JSONObject().apply {
                put("chat_id", chatId)
                put("text", alertText)
                put("parse_mode", "HTML")
                put("reply_markup", keyboardJson)
            }

            val url = URL("https://api.telegram.org/bot$botToken/sendMessage")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; utf-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            conn.responseCode
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send chat alert to Telegram: ${e.message}")
        }
    }
}
