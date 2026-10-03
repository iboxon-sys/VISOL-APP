package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// VISOL LUXURY MODERN DATING & ROMANCE COLOR PALETTE
// =========================================================================

// Primary & Romance Accent Tones
val VelvetRose = Color(0xFFE11D48)         // Deep Velvet Rose / Red
val VelvetRosePrimary = VelvetRose         // Primary alias
val VelvetRoseDark = Color(0xFFBE123C)     // Deep Rose shade
val VelvetRoseLight = Color(0xFFFB7185)    // Soft Rose glow
val VelvetRoseSoft = Color(0xFFFFF1F2)     // Ultra soft pinkish blush

// Warm Luxury Gold Accents
val WarmGold = Color(0xFFF59E0B)           // Radiant Soft Gold
val WarmGoldDark = Color(0xFFD97706)       // Rich Warm Gold
val WarmGoldLight = Color(0xFFFDE68A)      // Shimmering light gold

// Canvas & Glassmorphic Backgrounds
val PearlBackground = Color(0xFFFAF7F8)    // Light Pearl Background
val RomanticBgLight = Color(0xFFFAF5F5)    // Soft Pearl
val RomanticBlush = Color(0xFFFFF0F2)      // Warm Blush
val ChocolateDarkBg = Color(0xFF2B1A12)     // Qora fonda shokolad rang fon
val PureWhiteBg = Color(0xFFFFFFFF)        // Oq fonda prosta oq
val DeepNavyMesh = Color(0xFF13091B)       // Sokin chuqur to'q binafsha canvas
val DeepVelvetPurple = Color(0xFF38123D)   // Deep Velvet Plum mesh
val MutedBurgundy = Color(0xFF5E1333)      // Muted Burgundy mesh

// Surface & Text
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceCard = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1A1022)        // Deep Plum/Slate
val TextSecondary = Color(0xFF755E6F)      // Muted Plum/Slate
val TextWhite = Color(0xFFFFFFFF)
val TextMuted = Color(0xFF9E8597)

// =========================================================================
// WARM MODERN & SOFT GLASS PALETTE (Tanishuv & Marriage Luxury)
// =========================================================================

// Deep Sokin To'q Binafsha / Boy Shokolad (Warm Dark Canvas & Surfaces)
val WarmPlumDeep = Color(0xFF13091B)          // Chuqur sokin to'q binafsha (#13091B)
val WarmChocoPlumDark = Color(0xFF13091B)      // Deep Velvety Dark Violet & Espresso (#13091B)
val WarmChocoPlumMedium = Color(0xFF1F112B)    // Medium Plum Canvas (#1F112B)
val WarmChocoPlumSurface = Color(0xFF271636)   // Soft Glass Dark Surface (#271636)
val WarmChocoPlumElevated = Color(0xFF331E46)  // Elevated Plum Card Surface (#331E46)
val WarmChocoGlassDark = Color(0xEB1F112B)     // Beautiful Rich Semi-Opaque Plum Glass
val WarmChocoGlassLight = Color(0xEEFFFFFF)    // High frosted glass

// Warm Peach, Soft Romantic Rose, and Honey Gold Accents
val SoftPeach = Color(0xFFFF7B54)              // Yumshoq shaftoli (#FF7B54)
val WarmPeach = SoftPeach                      // Primary Warm Accent
val WarmPeachPrimary = SoftPeach
val WarmPeachSoft = Color(0xFFFFECE5)          // Mayin shaftoli blush
val WarmPeachLight = Color(0xFFFFAB91)         // Shaftoli porlashi
val SoftApricot = Color(0xFFFFA07A)            // Soft Apricot Glow
val WarmRosePink = Color(0xFFFF4D79)           // Issiq pushti (#FF4D79)
val SoftRosePink = WarmRosePink                // Mayin samimiy pushti / Velvet Rose
val SoftRosePinkGlow = Color(0xFFFF7A9F)       // Mayin pushti aksent
val WarmHoneyGold = Color(0xFFFBBF24)          // Iliq asal / yumshoq tilla aksent (#FBBF24)
val WarmHoneyGoldLight = Color(0xFFFDE68A)     // Mayin nurli tilla
val WarmCreamSoft = Color(0xFFFFF7ED)          // Qaymoq iliq fon
val WarmPlumViolet = Color(0xFF6B186F)         // Chuqur iliq binafsha

// Soft Glass Borders & Luminous Specular Accents
val SoftGlassBorderDark = Color(0x45FF7B54)    // Iliq shaftoli-pushti mayin hoshiya
val SoftGlassBorderLight = Color(0x35FF4D79)   // Mayin pushti hoshiya
val SoftGlassBorderGold = Color(0x35FBBF24)    // Mayin tilla hoshiya

// Compatibility Aliases for smooth integration
val CyberRose = SoftRosePink
val CyberPinkGlow = SoftRosePinkGlow
val CyberViolet = WarmPeach
val CyberCyan = WarmHoneyGold
val CyberElectricTeal = WarmPeachLight
val Y2KDarkCanvas = WarmChocoPlumDark
val Y2KGlassDark = WarmChocoGlassDark
val Y2KGlassLight = WarmChocoGlassLight
val Y2KBorderGlow = SoftGlassBorderDark
val Y2KBorderCyan = SoftGlassBorderGold
val Y2KBorderViolet = SoftGlassBorderDark

// Theme adaptive token getters
fun getAppCardBackground(isDark: Boolean) = if (isDark) WarmChocoGlassDark else WarmChocoGlassLight
fun getAppCardBorder(isDark: Boolean) = if (isDark) SoftGlassBorderDark else SoftGlassBorderLight
fun getAppInputBackground(isDark: Boolean) = if (isDark) Color(0xFF261A2E) else Color(0xFFFFF7ED)
fun getAppTextPrimary(isDark: Boolean) = if (isDark) Color(0xFFFDF4F5) else Color(0xFF1E1424)
fun getAppTextSecondary(isDark: Boolean) = if (isDark) Color(0xFFD4B8C7) else Color(0xFF786270)
fun getAppSurfaceElevated(isDark: Boolean) = if (isDark) WarmChocoPlumElevated else Color(0xFFFFFFFF)
fun getAppGlassPanel(isDark: Boolean) = if (isDark) Color(0xD8201528) else Color(0xF2FFFFFF)


// Glassmorphic Overlays & Borders
val GlassWhiteHigh = Color(0xEEFFFFFF)     // High-opacity frosted glass (93%)
val GlassWhiteMed = Color(0xCCFFFFFF)      // Medium frosted glass (80%)
val GlassWhiteLight = Color(0x66FFFFFF)    // Low frosted glass (40%)
val GlassWhiteSubtle = Color(0x26FFFFFF)   // Very subtle glass (15%)
val GlassBorderLight = Color(0x66FFFFFF)   // Light reflecting 1px border (40% white)
val GlassBorderRose = Color(0x4DE11D48)    // 30% velvet rose border
val GlassBorderGold = Color(0x66F59E0B)    // 40% warm gold border
val GlassDarkPanel = Color(0x14FFFFFF)     // rgba(255, 255, 255, 0.08)

// Legacy compatibility aliases for existing components
val EmeraldDark = Color(0xFF0F172A)
val EmeraldPrimary = VelvetRose
val EmeraldLight = Color(0xFF10B981)
val TurquoiseRoyal = Color(0xFF0284C7)
val TurquoiseDeep = Color(0xFF0369A1)
val GoldAccent = WarmGoldDark
val GoldLight = WarmGold
val PomegranateRed = VelvetRose
val CreamBackground = PearlBackground
val BorderGold = Color(0xFFE2E8F0)
val LuxuryNavy = DeepNavyMesh
val LuxuryNavyDark = Color(0xFF090D16)
val LuxuryRoseAccent = VelvetRose
val LuxuryGoldAccent = WarmGoldDark
val LuxuryBgLight = PearlBackground
val GlassWhite = GlassWhiteMed
val GlassBorder = GlassBorderLight
