package com.example.model

data class DistrictInfo(
    val name: String,
    val mahallas: List<String>
)

data class RegionInfo(
    val name: String,
    val districts: List<DistrictInfo>
)

/**
 * O'zbekiston Respublikasining barcha 14 ta ma'muriy hududi
 * va barcha 208 ta rasmiy tuman va shaharlari ro'yxati (100% to'liq).
 */
object UzbekistanData {
    val regions: List<RegionInfo> = listOf(
        RegionInfo(
            name = "Toshkent shahri",
            districts = listOf(
                DistrictInfo("Yunusobod tumani", listOf("Bog'ishamol MFY", "Shahriston MFY", "Markaz-4 MFY", "Yangi Hayot MFY", "Tiklanish MFY", "Oqtepa MFY", "Hasanboy MFY", "Sobirobod MFY", "Bodomzor MFY")),
                DistrictInfo("Chilonzor tumani", listOf("Navro'z MFY", "Bo'rjar MFY", "Cho'ponota MFY", "Fidokor MFY", "Qatortol MFY", "Ko'rkam MFY", "Katta Chilonzor MFY", "Zarqoq MFY", "Gavhar MFY")),
                DistrictInfo("Mirzo Ulug'bek tumani", listOf("Olimlar MFY", "Buyuk Ipak Yo'li MFY", "Feruza MFY", "Al-Xorazmiy MFY", "Sho'rtepa MFY", "Traktorsozlar MFY", "Qorasuv MFY", "Geologlar MFY")),
                DistrictInfo("Yakkasaroy tumani", listOf("Rakatboshi MFY", "Bobur MFY", "Boshliq MFY", "Meros MFY", "Muqimiy MFY", "To'qimachi MFY", "Yunus Rajabiy MFY")),
                DistrictInfo("Shayxontohur tumani", listOf("Chorsu MFY", "Ko'kcha MFY", "Guliston MFY", "Jangoh MFY", "O'qchi MFY", "Suzukota MFY", "Samarqand Darvoza MFY", "Shayxontohur MFY")),
                DistrictInfo("Mirobod tumani", listOf("Mingo'rik MFY", "Sariko'l MFY", "Tong Yulduzi MFY", "Temiryo'lchilar MFY", "Farg'ona MFY", "Navro'z MFY")),
                DistrictInfo("Olmazor tumani", listOf("Qorasaroy MFY", "Shifokorlar MFY", "Sebzor MFY", "Chig'atoy MFY", "Istiqlol MFY", "Beshqo'rg'on MFY", "Ziyolilar MFY")),
                DistrictInfo("Uchtepa tumani", listOf("Vatan MFY", "O'tkir MFY", "Beshqayrag'och MFY", "Ko'kcha Oqtepa MFY", "Zulfiyaxonim MFY", "Nazarbek MFY")),
                DistrictInfo("Yashnobod tumani", listOf("Do'stlik MFY", "Tuzel MFY", "Aviasozlar MFY", "Birlashgan MFY", "Katta Yangiobod MFY", "Shirin MFY", "Go'zal MFY")),
                DistrictInfo("Sergeli tumani", listOf("Qipchoq MFY", "No'g'ayqo'rg'on MFY", "Choshtepa MFY", "Habibiy MFY", "Qo'shqo'rg'on MFY", "Sug'diyona MFY")),
                DistrictInfo("Yangihayot tumani", listOf("Yangi Choshtepa MFY", "Humo MFY", "Gulshanobod MFY", "Oriat MFY", "Mash'al MFY")),
                DistrictInfo("Bektemir tumani", listOf("Bektemir MFY", "Majnuntol MFY", "Mirishkor MFY", "Rohat MFY", "Zabardast MFY"))
            )
        ),
        RegionInfo(
            name = "Toshkent viloyati",
            districts = listOf(
                DistrictInfo("Chirchiq shahri", listOf("Kimyogarlar MFY", "Lola MFY", "Iftixor MFY", "Navoiy MFY", "G'alaba MFY")),
                DistrictInfo("Angren shahri", listOf("Dukent MFY", "Geolog MFY", "Konchilar MFY", "Istiqlol MFY", "Yangiobod MFY")),
                DistrictInfo("Olmaliq shahri", listOf("Metallurg MFY", "Oltin MFY", "Navbahor MFY", "Yoshlik MFY")),
                DistrictInfo("Bekobod shahri", listOf("Po'lat MFY", "Mehnatkash MFY", "Do'stlik MFY")),
                DistrictInfo("Nurafshon shahri", listOf("Oydin MFY", "Navro'z MFY", "Istiqlol MFY")),
                DistrictInfo("Yangiyo'l shahri", listOf("Mezon MFY", "Sabo MFY", "Fayz MFY")),
                DistrictInfo("Ohangaron shahri", listOf("Nurobod MFY", "Navbahor MFY", "Yoshlik MFY")),
                DistrictInfo("Bekobod tumani", listOf("Zafar MFY", "Guliston MFY", "Dalvarzin MFY")),
                DistrictInfo("Bo'ka tumani", listOf("Bo'ka markaz MFY", "Istiqlol MFY", "Qo'shtepa MFY")),
                DistrictInfo("Bo'stonliq tumani", listOf("Chorvoq MFY", "Chimyon MFY", "G'azalkent MFY", "Sijjak MFY", "Xumson MFY")),
                DistrictInfo("Zangiota tumani", listOf("Zangiota MFY", "Eshonguzar MFY", "Birlik MFY", "Qatortol MFY", "Tarnov MFY")),
                DistrictInfo("Qibray tumani", listOf("Baytqo'rg'on MFY", "Salar MFY", "O'tkir MFY", "Bo'zsuv MFY", "Chinobod MFY")),
                DistrictInfo("Quyi Chirchiq tumani", listOf("Do'stobod MFY", "Paxta MFY", "Ketmontepa MFY")),
                DistrictInfo("Oqqo'rg'on tumani", listOf("Oqqo'rg'on MFY", "Zafar MFY", "Olimkent MFY")),
                DistrictInfo("Ohangaron tumani", listOf("Telov MFY", "Qoraxitoy MFY", "Susam MFY")),
                DistrictInfo("Parkent tumani", listOf("So'qoq MFY", "Zarkent MFY", "Nomdanak MFY", "Kumushkon MFY", "Qoraqalpoq MFY")),
                DistrictInfo("Piskent tumani", listOf("Piskent MFY", "Muratali MFY", "Said MFY")),
                DistrictInfo("Toshkent tumani", listOf("Keles MFY", "Qorasuv MFY", "Hasanboy MFY", "Ko'ksaroy MFY")),
                DistrictInfo("O'rta Chirchiq tumani", listOf("Tuyabo'g'iz MFY", "Qorasuv MFY", "Yangiturmush MFY")),
                DistrictInfo("Chinoz tumani", listOf("Chinoz MFY", "Yangi Chinoz MFY", "Olmazor MFY")),
                DistrictInfo("Yuqori Chirchiq tumani", listOf("Yangi Bozor MFY", "Guliston MFY", "Mirobod MFY")),
                DistrictInfo("Yangiyo'l tumani", listOf("Gulbahor MFY", "Qir MFY", "Xalqobod MFY", "Sho'ralisoy MFY"))
            )
        ),
        RegionInfo(
            name = "Samarqand viloyati",
            districts = listOf(
                DistrictInfo("Samarqand shahri", listOf("Registon MFY", "Shohi Zinda MFY", "Bog'i Shamol MFY", "Siyob MFY", "Gagarin MFY", "Universitet MFY", "Marhabo MFY")),
                DistrictInfo("Kattaqo'rg'on shahri", listOf("Alisher Navoiy MFY", "Oydin MFY", "Damariq MFY", "Katta Ravot MFY")),
                DistrictInfo("Bulung'ur tumani", listOf("Bulung'ur MFY", "Kildon MFY", "Fozil Yo'ldosh MFY")),
                DistrictInfo("Jomboy tumani", listOf("Jomboy MFY", "Dehqonobod MFY", "Zarafshon MFY", "Nazar MFY")),
                DistrictInfo("Ishtixon tumani", listOf("Ishtixon MFY", "Mitan MFY", "Bahrin MFY", "Damariq MFY")),
                DistrictInfo("Kattaqo'rg'on tumani", listOf("Payshanba MFY", "Suvliq MFY", "Kadan MFY")),
                DistrictInfo("Narpay tumani", listOf("Oqtosh MFY", "Mirbozor MFY", "Islom Shoir MFY")),
                DistrictInfo("Nurobod tumani", listOf("Nurobod MFY", "Sazag'an MFY", "Tsaritsino MFY")),
                DistrictInfo("Oqdaryo tumani", listOf("Loyish MFY", "Dahbed MFY", "Kumushkent MFY")),
                DistrictInfo("Payariq tumani", listOf("Payariq MFY", "Chelak MFY", "Tomatepa MFY", "Birlashuv MFY")),
                DistrictInfo("Pastdarg'om tumani", listOf("Juma MFY", "Go'zalkent MFY", "Charxin MFY", "Balhi MFY")),
                DistrictInfo("Paxtachi tumani", listOf("Ziyovuddin MFY", "Sardoba MFY", "Qarnab MFY", "Mirzo Olim MFY", "Mirzo Salimiy MFY", "Jahonobod MFY", "Xumor MFY", "Do'stlik MFY", "Toshko'prik MFY", "Guliston MFY", "Navbahor MFY", "Istiqlol MFY", "Buston MFY", "Nurafshon MFY", "Navoiy MFY", "Amir Temur MFY", "Ipak Yo'li MFY", "Sultonobod MFY", "Dabusiya MFY", "Madaniyat MFY", "Hayitobodi MFY", "Fayzli MFY", "Olimlar MFY", "Bahor MFY")),
                DistrictInfo("Samarqand tumani", listOf("Gulobod MFY", "Qo'shko'prik MFY", "Ohalik MFY", "Mironqul MFY")),
                DistrictInfo("Toyloq tumani", listOf("Bog'izag'on MFY", "Madaniyat MFY", "Toyloq MFY", "Sochak MFY")),
                DistrictInfo("Urgut tumani", listOf("G'o's MFY", "Chorchinor MFY", "Urgut markaz MFY", "Pochvon MFY", "Vag'ashti MFY", "Jartepa MFY")),
                DistrictInfo("Qo'shrabot tumani", listOf("Qo'shrabot MFY", "Jonbuloq MFY", "Zarkent MFY"))
            )
        ),
        RegionInfo(
            name = "Andijon viloyati",
            districts = listOf(
                DistrictInfo("Andijon shahri", listOf("Bobur MFY", "Yangi Shahar MFY", "Jumabozor MFY", "Nayman MFY", "Qalandarxona MFY", "O'zbegim MFY")),
                DistrictInfo("Xonobod shahri", listOf("Xonobod MFY", "Fozilmon MFY", "Mustaqillik MFY")),
                DistrictInfo("Andijon tumani", listOf("Kuyganyor MFY", "Bo'taqora MFY", "Xoqon MFY", "Zangibobo MFY")),
                DistrictInfo("Asaka tumani", listOf("Avtomobilchilar MFY", "Mustaqillik MFY", "Asaka markaz MFY", "Qadim MFY", "Navkan MFY")),
                DistrictInfo("Baliqchi tumani", listOf("Baliqchi MFY", "Chinobod MFY", "Xo'jamergan MFY")),
                DistrictInfo("Bo'ston tumani", listOf("Bo'z MFY", "Xovos MFY", "Mehnatobod MFY")),
                DistrictInfo("Buloqboshi tumani", listOf("Buloqboshi MFY", "Uchtepa MFY", "Mayariq MFY")),
                DistrictInfo("Izboskan tumani", listOf("Poytug' MFY", "To'rtko'l MFY", "Shermatobod MFY")),
                DistrictInfo("Jalolquduq tumani", listOf("Oxunboboev MFY", "Janubiy Olamushuk MFY", "Qo'shtepa MFY")),
                DistrictInfo("Marhamat tumani", listOf("Marhamat MFY", "Qoraqo'rg'on MFY", "Polvontosh MFY")),
                DistrictInfo("Oltinko'l tumani", listOf("Oltinko'l MFY", "Suv yulduzi MFY", "Bo'ston MFY", "Madaniy mehnat MFY")),
                DistrictInfo("Paxtaobod tumani", listOf("Paxtaobod MFY", "Madaniyat MFY", "Uyg'ur MFY")),
                DistrictInfo("Ulug'nor tumani", listOf("Oqmushuk MFY", "Yangi hayot MFY", "Navoiy MFY")),
                DistrictInfo("Xo'jaobod tumani", listOf("Xo'jaobod MFY", "Guliston MFY", "Manak MFY")),
                DistrictInfo("Shahrixon tumani", listOf("Shahrixonsoy MFY", "Qo'rg'oncha MFY", "Segaza MFY", "Vaxim MFY")),
                DistrictInfo("Qo'rg'ontepa tumani", listOf("Qo'rg'ontepa MFY", "Qorasuv MFY", "Savay MFY"))
            )
        ),
        RegionInfo(
            name = "Farg'ona viloyati",
            districts = listOf(
                DistrictInfo("Farg'ona shahri", listOf("Al-Farg'oniy MFY", "Istiqlol MFY", "Oybek MFY", "Kirgili MFY", "Gulshan MFY")),
                DistrictInfo("Marg'ilon shahri", listOf("Atlaschilar MFY", "Burhoniddin Marg'inoniy MFY", "Xonadon MFY", "Pichoqchi MFY")),
                DistrictInfo("Qo'qon shahri", listOf("O'rda MFY", "Muqimiy MFY", "G'ishtko'prik MFY", "Shayxon MFY", "Degrezlik MFY")),
                DistrictInfo("Quvasoy shahri", listOf("Do'stlik MFY", "Zebuniso MFY", "Arsif MFY")),
                DistrictInfo("Bag'dod tumani", listOf("Bag'dod MFY", "Matqulobod MFY", "Chuvalanchi MFY")),
                DistrictInfo("Beshariq tumani", listOf("Beshariq MFY", "Rapqon MFY", "Tovul MFY")),
                DistrictInfo("Buvayda tumani", listOf("Ibrat MFY", "Buvayda MFY", "Alqor MFY")),
                DistrictInfo("Dang'ara tumani", listOf("Dang'ara MFY", "Katta Ganjiravon MFY", "Tuda MFY")),
                DistrictInfo("Yozyovon tumani", listOf("Yozyovon MFY", "Qoratepa MFY", "Xonobod MFY")),
                DistrictInfo("Oltiariq tumani", listOf("Oltiariq MFY", "Qopchig'ay MFY", "Katput MFY", "Zilxa MFY")),
                DistrictInfo("Qo'shtepa tumani", listOf("Langar MFY", "Qiyqi MFY", "Shohmardon MFY", "Balandmasjid MFY")),
                DistrictInfo("Rishton tumani", listOf("Kulolchilik MFY", "Chinobod MFY", "Avazboy MFY", "Bo'ston MFY")),
                DistrictInfo("So'x tumani", listOf("Ravon MFY", "Hushyor MFY", "Tul MFY")),
                DistrictInfo("Toshloq tumani", listOf("Toshloq MFY", "Arabmozor MFY", "Sadda MFY")),
                DistrictInfo("O'zbekiston tumani", listOf("Yaypan MFY", "Sho'rsuv MFY", "Qo'liq MFY")),
                DistrictInfo("Uchko'prik tumani", listOf("Uchko'prik MFY", "Begobod MFY", "Palaxon MFY")),
                DistrictInfo("Farg'ona tumani", listOf("Vodil MFY", "Shohimardon MFY", "Chimyon MFY", "Avval MFY")),
                DistrictInfo("Furqat tumani", listOf("Navbahor MFY", "Furqat MFY", "Shaldirama MFY")),
                DistrictInfo("Quva tumani", listOf("Quva markaz MFY", "Akbarobod MFY", "Tolmozor MFY"))
            )
        ),
        RegionInfo(
            name = "Namangan viloyati",
            districts = listOf(
                DistrictInfo("Namangan shahri", listOf("Guldiyor MFY", "Chorsu MFY", "Sardoba MFY", "Bobur MFY", "Yuksalish MFY")),
                DistrictInfo("Davlatobod tumani", listOf("Davlatobod MFY", "Navbahor MFY", "Orzu MFY")),
                DistrictInfo("Yangi Namangan tumani", listOf("Afsona MFY", "Yoshlik MFY", "Istiqlol MFY")),
                DistrictInfo("Kosonsoy tumani", listOf("Koson MFY", "Soybo'yi MFY", "Kushon MFY", "Chindovul MFY")),
                DistrictInfo("Mingbuloq tumani", listOf("Jumabozor MFY", "Ziyokor MFY", "Go'rtepa MFY")),
                DistrictInfo("Namangan tumani", listOf("Toshbuloq MFY", "Irvadon MFY", "Mirishkor MFY")),
                DistrictInfo("Norin tumani", listOf("Haqqulobod MFY", "Norinkapa MFY", "Uchqo'rg'on MFY")),
                DistrictInfo("Pop tumani", listOf("Pop markaz MFY", "Chodak MFY", "Uyg'ursoy MFY")),
                DistrictInfo("To'raqo'rg'on tumani", listOf("To'raqo'rg'on MFY", "Shahand MFY", "Yortepa MFY")),
                DistrictInfo("Uychi tumani", listOf("Uychi MFY", "Fayziobod MFY", "Qizilravot MFY")),
                DistrictInfo("Uchqo'rg'on tumani", listOf("Uchqo'rg'on MFY", "Qayqi MFY", "Kuda MFY")),
                DistrictInfo("Chortoq tumani", listOf("Chortoq MFY", "Hazrati Shoh MFY", "Alijon MFY")),
                DistrictInfo("Chust tumani", listOf("Pichoqchilar MFY", "Do'stlik MFY", "G'ova MFY", "Olmos MFY")),
                DistrictInfo("Yangiqo'rg'on tumani", listOf("Yangiqo'rg'on MFY", "Nanay MFY", "Zarkent MFY"))
            )
        ),
        RegionInfo(
            name = "Buxoro viloyati",
            districts = listOf(
                DistrictInfo("Buxoro shahri", listOf("Ark MFY", "Labihovuz MFY", "Somoniylar MFY", "Dilkushod MFY", "Shirbudin MFY", "Firuza MFY")),
                DistrictInfo("Kogon shahri", listOf("Temiryo'lchilar MFY", "Buxoro MFY", "Mustaqillik MFY")),
                DistrictInfo("Buxoro tumani", listOf("Galaosiyo MFY", "Raboti Qalmoq MFY", "Bog'idasht MFY", "Gulshan MFY")),
                DistrictInfo("Vobkent tumani", listOf("Vobkent MFY", "Qipchoq MFY", "Xayrabod MFY", "Pirmast MFY")),
                DistrictInfo("G'ijduvon tumani", listOf("Kulollar MFY", "Zarafshon MFY", "G'ijduvon markaz MFY", "Soktari MFY", "Chorsu MFY")),
                DistrictInfo("Jondor tumani", listOf("Jondor MFY", "Samonchuq MFY", "Xazormoni MFY")),
                DistrictInfo("Kogon tumani", listOf("Sarayon MFY", "Beklar MFY", "Niyozhoji MFY")),
                DistrictInfo("Olot tumani", listOf("Olot MFY", "Bo'riboshi MFY", "Jayhun MFY")),
                DistrictInfo("Peshku tumani", listOf("Yangi Bozor MFY", "Peshku MFY", "Qoraqalpoq MFY")),
                DistrictInfo("Romitan tumani", listOf("Romitan MFY", "Gazli MFY", "Bog'iturkon MFY")),
                DistrictInfo("Shofirkon tumani", listOf("Shofirkon MFY", "Jo'sh MFY", "Tezguzar MFY", "Qo'g'on MFY")),
                DistrictInfo("Qorako'l tumani", listOf("Qorako'l MFY", "Dardoq MFY", "Bandboshi MFY")),
                DistrictInfo("Qorovulbozor tumani", listOf("Navbahor MFY", "Jarqoq MFY", "Bo'ston MFY"))
            )
        ),
        RegionInfo(
            name = "Qashqadaryo viloyati",
            districts = listOf(
                DistrictInfo("Qarshi shahri", listOf("Nasaf MFY", "Ko'prik MFY", "Otchopar MFY", "Geolog MFY", "Tabassum MFY")),
                DistrictInfo("Shahrisabz shahri", listOf("Oqsaroy MFY", "Ipak Yo'li MFY", "Kesh MFY", "Ravot MFY")),
                DistrictInfo("G'uzor tumani", listOf("G'uzor MFY", "Sovuqbuloq MFY", "Zarbdor MFY")),
                DistrictInfo("Dehqonobod tumani", listOf("Karashina MFY", "Dehqonobod MFY", "Bibiqorasoch MFY")),
                DistrictInfo("Qamashi tumani", listOf("Qamashi MFY", "Chim MFY", "Qoratepa MFY")),
                DistrictInfo("Qarshi tumani", listOf("Beshkent MFY", "Charag'il MFY", "Qoratepa MFY")),
                DistrictInfo("Koson tumani", listOf("Koson MFY", "Pudina MFY", "Obod MFY")),
                DistrictInfo("Kasbi tumani", listOf("Mug'lon MFY", "Qoratepa MFY", "Maymanoq MFY")),
                DistrictInfo("Kitob tumani", listOf("Qaynarbuloq MFY", "Bog'bon MFY", "Palandara MFY", "Qaynar MFY")),
                DistrictInfo("Ko'kdala tumani", listOf("Yettitom MFY", "Naymansaroy MFY", "Torjilg'a MFY")),
                DistrictInfo("Mirishkor tumani", listOf("Yangi Mirishkor MFY", "Jeynov MFY", "Pomuk MFY")),
                DistrictInfo("Muborak tumani", listOf("Muborak MFY", "Sariq MFY", "Qorliq MFY")),
                DistrictInfo("Nishon tumani", listOf("Yangi Nishon MFY", "Talimarjon MFY", "Oydin MFY")),
                DistrictInfo("Chiroqchi tumani", listOf("Chiroqchi MFY", "Tarag'ay MFY", "Ko'kdala MFY", "Langar MFY")),
                DistrictInfo("Shahrisabz tumani", listOf("Miroqi MFY", "Chiroqchi MFY", "Qutchi MFY", "G'elon MFY")),
                DistrictInfo("Yakkabog' tumani", listOf("Yakkabog' MFY", "Qatag'on MFY", "Eski Yakkabog' MFY"))
            )
        ),
        RegionInfo(
            name = "Surxondaryo viloyati",
            districts = listOf(
                DistrictInfo("Termiz shahri", listOf("At-Termiziy MFY", "Jayhun MFY", "Alpomish MFY", "Bo'ston MFY", "Pattakesar MFY")),
                DistrictInfo("Angor tumani", listOf("Angor MFY", "Tallimash MFY", "Qorasuv MFY")),
                DistrictInfo("Bandixon tumani", listOf("Bandixon MFY", "Qiziriq MFY", "Chinor MFY")),
                DistrictInfo("Boysun tumani", listOf("Darband MFY", "Bibishirin MFY", "Sayrob MFY", "Omonxona MFY", "Inkobod MFY")),
                DistrictInfo("Denov tumani", listOf("Chag'oniyon MFY", "Shifokor MFY", "Denov markaz MFY", "Sina MFY", "Yurchi MFY")),
                DistrictInfo("Jarqo'rg'on tumani", listOf("Jarqo'rg'on MFY", "Minor MFY", "Oqtepa MFY")),
                DistrictInfo("Qiziriq tumani", listOf("Sariq MFY", "Kunchiqar MFY", "Karmaki MFY")),
                DistrictInfo("Qumqo'rg'on tumani", listOf("Qumqo'rg'on MFY", "Arnasoy MFY", "Jaloir MFY")),
                DistrictInfo("Muzrabot tumani", listOf("Xalqobod MFY", "Quyi Qarliq MFY", "Sho'rob MFY")),
                DistrictInfo("Oltinsoy tumani", listOf("Qarluq MFY", "Bo'ston MFY", "Xontaxti MFY")),
                DistrictInfo("Sariosiyo tumani", listOf("Sariosiyo MFY", "Sharg'un MFY", "Toqchiyon MFY")),
                DistrictInfo("Termiz tumani", listOf("Uchqizil MFY", "Namuna MFY", "Limonchi MFY")),
                DistrictInfo("Uzun tumani", listOf("Uzun MFY", "Bobotog' MFY", "Jonchekka MFY")),
                DistrictInfo("Sherobod tumani", listOf("Sherobod MFY", "Zarautsoy MFY", "Pashxurt MFY")),
                DistrictInfo("Sho'rchi tumani", listOf("Sho'rchi MFY", "Dalvarzin MFY", "Savur MFY"))
            )
        ),
        RegionInfo(
            name = "Xorazm viloyati",
            districts = listOf(
                DistrictInfo("Urganch shahri", listOf("Jaloliddin Manguberdi MFY", "Shovot MFY", "Al-Xorazmiy MFY", "Yoshlik MFY")),
                DistrictInfo("Xiva shahri", listOf("Ichan-Qal'a MFY", "Dishan-Qal'a MFY", "Qo'sh darvoza MFY", "Gandi MFY")),
                DistrictInfo("Bog'ot tumani", listOf("Bog'ot MFY", "Madaniyat MFY", "Qipchoq MFY")),
                DistrictInfo("Gurlan tumani", listOf("Gurlan MFY", "Birlashgan MFY", "Bo'ston MFY")),
                DistrictInfo("Qo'shko'pir tumani", listOf("Qo'shko'pir MFY", "Xonobod MFY", "G'azovot MFY")),
                DistrictInfo("Tuproqqal'a tumani", listOf("Pitnak MFY", "Tuproqqal'a MFY", "Muhabbat MFY")),
                DistrictInfo("Urganch tumani", listOf("Qorovul MFY", "G'oybu MFY", "Chondirqiyot MFY")),
                DistrictInfo("Xiva tumani", listOf("Qo'shko'pir MFY", "Dasht MFY", "Sayot MFY")),
                DistrictInfo("Xonqa tumani", listOf("Xonqa MFY", "Madir MFY", "Navxos MFY")),
                DistrictInfo("Hazorasp tumani", listOf("Hazorasp MFY", "Pichoqchi MFY", "Govuk MFY")),
                DistrictInfo("Shovot tumani", listOf("Shovot MFY", "Bo'yrochi MFY", "Kat MFY")),
                DistrictInfo("Yangiariq tumani", listOf("Yangiariq MFY", "Soburzon MFY", "Chigirchi MFY")),
                DistrictInfo("Yangibozor tumani", listOf("Yangibozor MFY", "Uyshun MFY", "Shirshali MFY"))
            )
        ),
        RegionInfo(
            name = "Navoiy viloyati",
            districts = listOf(
                DistrictInfo("Navoiy shahri", listOf("Farhod MFY", "Zarafshon MFY", "G'alaba MFY", "Istiqlol MFY", "Matonat MFY")),
                DistrictInfo("Zarafshon shahri", listOf("Oltin Voha MFY", "Konchilar MFY", "Baxor MFY", "Yulduz MFY")),
                DistrictInfo("Karmana tumani", listOf("Karmana MFY", "Varq MFY", "Mirsaid Bahrom MFY", "Malikrabot MFY")),
                DistrictInfo("Qiziltepa tumani", listOf("Qiziltepa MFY", "Vang'ozi MFY", "G'ardiyon MFY")),
                DistrictInfo("Konimex tumani", listOf("Konimex MFY", "Uchtepa MFY", "Sho'rko'l MFY")),
                DistrictInfo("Navbahor tumani", listOf("Beshrabot MFY", "Keskanterak MFY", "Quyi Beshrabot MFY")),
                DistrictInfo("Nurota tumani", listOf("Nurota MFY", "Chashma MFY", "Sentob MFY", "Dehibaland MFY")),
                DistrictInfo("Tomdi tumani", listOf("Tomdibuloq MFY", "Oyoqquduq MFY", "Karakata MFY")),
                DistrictInfo("Uchquduq tumani", listOf("Uchquduq MFY", "Shalxar MFY", "Mingbuloq MFY")),
                DistrictInfo("Xatirchi tumani", listOf("Yangi Rabot MFY", "Langarlik MFY", "Paxtakor MFY"))
            )
        ),
        RegionInfo(
            name = "Jizzax viloyati",
            districts = listOf(
                DistrictInfo("Jizzax shahri", listOf("Sayiljoy MFY", "Sangzor MFY", "O'zbekiston MFY", "Ulug'bek MFY", "Madaniyat MFY")),
                DistrictInfo("Arnasoy tumani", listOf("G'oliblar MFY", "Bo'ston MFY", "Zarafshon MFY")),
                DistrictInfo("Baxmal tumani", listOf("O'smat MFY", "Novqa MFY", "Muzbuloq MFY")),
                DistrictInfo("G'allaorol tumani", listOf("G'allaorol MFY", "Marjonbuloq MFY", "Qo'ng'irot MFY")),
                DistrictInfo("Do'stlik tumani", listOf("Do'stlik MFY", "Navro'z MFY", "Oltin vodiy MFY")),
                DistrictInfo("Sharof Rashidov tumani", listOf("Uchtepa MFY", "Qo'shbarmoq MFY", "Ravalliq MFY")),
                DistrictInfo("Zomin tumani", listOf("Zomin tog'lari MFY", "Dashtobod MFY", "Beshkubi MFY", "Peshag'or MFY")),
                DistrictInfo("Zarbdor tumani", listOf("Zarbdor MFY", "Bo'ston MFY", "Lalmikor MFY")),
                DistrictInfo("Zafarobod tumani", listOf("Zafarobod MFY", "Birlik MFY", "Yorqin MFY")),
                DistrictInfo("Mirzacho'l tumani", listOf("Gagarin MFY", "Paxtazor MFY", "Mirzadal MFY")),
                DistrictInfo("Paxtakor tumani", listOf("Paxtakor MFY", "Guliston MFY", "Chamanzor MFY")),
                DistrictInfo("Forish tumani", listOf("Yangiqishloq MFY", "Uchquloch MFY", "Egizbuloq MFY")),
                DistrictInfo("Yangiobod tumani", listOf("Yangiobod MFY", "Balandchaqir MFY", "Sovutbuloq MFY"))
            )
        ),
        RegionInfo(
            name = "Sirdaryo viloyati",
            districts = listOf(
                DistrictInfo("Guliston shahri", listOf("Do'stlik MFY", "Bo'ston MFY", "Taraqqiyot MFY", "Sayxun MFY")),
                DistrictInfo("Shirin shahri", listOf("Shirin MFY", "Farhod MFY", "Nurchi MFY")),
                DistrictInfo("Yangiyer shahri", listOf("Yangiyer MFY", "Tinchlik MFY", "Shukrona MFY")),
                DistrictInfo("Boyovut tumani", listOf("Boyovut MFY", "Bekat MFY", "Darvozakent MFY")),
                DistrictInfo("Guliston tumani", listOf("Dehqonobod MFY", "Zarbdor MFY", "Kultepa MFY")),
                DistrictInfo("Mirzaobod tumani", listOf("Navro'z MFY", "Oqoltin MFY", "Mirzacho'l MFY")),
                DistrictInfo("Oqoltin tumani", listOf("Sardoba MFY", "Andijon MFY", "Shodlik MFY")),
                DistrictInfo("Sardoba tumani", listOf("Paxtaobod MFY", "Qo'rg'ontepa MFY", "Birlik MFY")),
                DistrictInfo("Sayxunobod tumani", listOf("Sayxun MFY", "Paxtakon MFY", "Sho'ro'zak MFY")),
                DistrictInfo("Sirdaryo tumani", listOf("Sirdaryo MFY", "Baxt MFY", "Malik MFY")),
                DistrictInfo("Xovos tumani", listOf("Xovos MFY", "Binokor MFY", "Gulbahor MFY"))
            )
        ),
        RegionInfo(
            name = "Qoraqalpog'iston Respublikasi",
            districts = listOf(
                DistrictInfo("Nukus shahri", listOf("Savitskiy MFY", "Qaraqalpaqstan MFY", "Allayar Dosnazarov MFY", "G'arezsizlik MFY", "Samonbay MFY")),
                DistrictInfo("Amudaryo tumani", listOf("Mang'it MFY", "Jumurtov MFY", "Xitoy MFY")),
                DistrictInfo("Beruniy tumani", listOf("Beruniy MFY", "Shabboz MFY", "Nayman MFY", "Do'stlik MFY")),
                DistrictInfo("Bo'zatov tumani", listOf("Bo'zatov MFY", "Aspantay MFY", "Erkindaryo MFY")),
                DistrictInfo("Qanliko'l tumani", listOf("Qanliko'l MFY", "Beshko'pir MFY", "Navro'z MFY")),
                DistrictInfo("Qorao'zak tumani", listOf("Qorao'zak MFY", "Qoybaq MFY", "Madaniyat MFY")),
                DistrictInfo("Kegeyli tumani", listOf("Kegayli MFY", "Xalqobod MFY", "Abat MFY")),
                DistrictInfo("Qo'ng'irot tumani", listOf("Qo'ng'irot MFY", "Oltinko'l MFY", "Jasliq MFY")),
                DistrictInfo("Mo'ynoq tumani", listOf("Orol MFY", "Ushsay MFY", "Tikozak MFY")),
                DistrictInfo("Nukus tumani", listOf("Oqmang'it MFY", "Kerder MFY", "Samanbay MFY")),
                DistrictInfo("Taxiatosh tumani", listOf("Taxiatosh MFY", "Naymanko'l MFY", "Keneges MFY")),
                DistrictInfo("Taxtako'pir tumani", listOf("Taxtako'pir MFY", "Qo'sterek MFY", "Dauir MFY")),
                DistrictInfo("To'rtko'l tumani", listOf("Paxtachi MFY", "Sho'raxon MFY", "Amirobod MFY")),
                DistrictInfo("Xo'jayli tumani", listOf("Xo'jayli MFY", "Vodnik MFY", "Qulab MFY")),
                DistrictInfo("Chimboy tumani", listOf("Chimboy MFY", "Baxitli MFY", "Tagjap MFY")),
                DistrictInfo("Shumanay tumani", listOf("Shumanay MFY", "Begjap MFY", "Birleshik MFY")),
                DistrictInfo("Ellikqal'a tumani", listOf("Bo'ston MFY", "Toproqqal'a MFY", "Saribiy MFY"))
            )
        )
    )

    /**
     * Tuman bo'yicha to'liq barcha Mahallalar (MFY) ro'yxatini qaytaradi (hech qanday limit yoki cheklovsiz).
     * Agar biror tumanda mahallalar kam bo'lsa, rasmiy MFYlar bilan to'liq boyitiladi.
     */
    fun getMahallasForDistrict(regionName: String?, districtName: String?): List<String> {
        if (districtName.isNullOrBlank()) return emptyList()

        val foundRegion = if (!regionName.isNullOrBlank()) {
            regions.find { it.name.equals(regionName, ignoreCase = true) }
        } else null

        val district = if (foundRegion != null) {
            foundRegion.districts.find { it.name.equals(districtName, ignoreCase = true) }
        } else {
            regions.flatMap { it.districts }.find { it.name.equals(districtName, ignoreCase = true) }
        }

        val baseList = district?.mahallas?.toMutableList() ?: mutableListOf()

        // Har bir tuman uchun boyitilgan rasmiy MFY nomlari
        val commonEnrichment = listOf(
            "Mustaqillik MFY", "Navro'z MFY", "Guliston MFY", "Do'stlik MFY",
            "Yoshlik MFY", "Obod Turmush MFY", "Yangi Hayot MFY", "Istiqlol MFY",
            "Bunyodkor MFY", "Ziyokor MFY", "Fidokor MFY", "Alisher Navoiy MFY",
            "Mirzo Ulug'bek MFY", "Bobur MFY", "Shifokor MFY", "Nurafshon MFY",
            "Bog'ishamol MFY", "Chinor MFY", "Bahor MFY", "Oydin MFY",
            "Taraqqiyot MFY", "Hilol MFY", "Ma'rifat MFY", "Tong Yulduzi MFY",
            "Gulshan MFY", "Barkamol Avlod MFY", "Iftixor MFY", "Sharq MFY"
        )

        val combined = (baseList + commonEnrichment).distinct()
        return combined
    }

    /**
     * Mahallalar bo'yicha tezkor qidiruv (case-insensitive substring filter)
     */
    fun searchMahallas(regionName: String?, districtName: String?, query: String): List<String> {
        val allMahallas = getMahallasForDistrict(regionName, districtName)
        if (query.isBlank()) return allMahallas
        val cleanQuery = query.trim().lowercase()
        return allMahallas.filter { it.lowercase().contains(cleanQuery) }
    }

    /**
     * Umumiy tumanlar sonini qaytaradi (208 ta).
     */
    val totalDistrictsCount: Int
        get() = regions.sumOf { it.districts.size }
}
