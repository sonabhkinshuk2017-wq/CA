package com.example.ui.skins

import androidx.compose.ui.graphics.Color

data class Skin(
    val id: String,
    val name: String,
    val category: String, // "Marvel", "DC", "Cartoons"
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val onPrimary: Color = Color.White,
    val onSurface: Color,
    val isDark: Boolean = true,
    val accent: Color,
    val border: Color,
    val fontFamilyLabel: String = "Mono" // "Compact", "Blocky", "Comic", "Hero", "Normal"
)

object SkinRepository {
    val skins = listOf(
        // === MARVEL UNIVERSE (15 Skins) ===
        Skin(
            id = "iron_man",
            name = "Iron Man",
            category = "Marvel",
            primary = Color(0xFF880E0F), // High Density Crimson Red
            secondary = Color(0xFFFFD700), // High Density Gold
            background = Color(0xFF1C1B1F), // High Density Charcoal Background
            surface = Color(0xFF2B2930), // High Density M3 Surface Container
            onPrimary = Color(0xFFFFD700), // Gold on Crimson
            onSurface = Color(0xFFE6E1E5), // Soft High Density White
            accent = Color(0xFFFFD700), // Glowing Arc Reactor/Gold Accents
            border = Color(0xFF49454F), // High Density M3 Border Color
            fontFamilyLabel = "Hero"
        ),
        Skin(
            id = "thor",
            name = "Thor",
            category = "Marvel",
            primary = Color(0xFF1E88E5), // Asgardian Blue
            secondary = Color(0xFFDCDCDC), // Silver
            background = Color(0xFF0B1220),
            surface = Color(0xFF19253E),
            onSurface = Color(0xFFE1F5FE),
            accent = Color(0xFFFFEB3B), // Lightning Yellow
            border = Color(0xFF90A4AE),
            fontFamilyLabel = "Hero"
        ),
        Skin(
            id = "spider_man",
            name = "Spider-Man",
            category = "Marvel",
            primary = Color(0xFFD32F2F), // Web Red
            secondary = Color(0xFF1976D2), // Spider Blue
            background = Color(0xFF0D0D14),
            surface = Color(0xFF1B1B2A),
            onSurface = Color(0xFFECEFF1),
            accent = Color(0xFFECEFF1), // Silver Webbing
            border = Color(0xFFD32F2F),
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "captain_america",
            name = "Captain America",
            category = "Marvel",
            primary = Color(0xFF0D47A1), // Patriot Blue
            secondary = Color(0xFFD32F2F), // Star Shield Red
            background = Color(0xFF0A0F1D),
            surface = Color(0xFF18223B),
            onSurface = Color(0xFFF5F5F5),
            accent = Color(0xFFFFFFFF),
            border = Color(0xFF0D47A1),
            fontFamilyLabel = "Hero"
        ),
        Skin(
            id = "black_panther",
            name = "Black Panther",
            category = "Marvel",
            primary = Color(0xFF4A148C), // Wakanda Purple
            secondary = Color(0xFF212121), // Charcoal
            background = Color(0xFF070707),
            surface = Color(0xFF161616),
            onSurface = Color(0xFFE1BEE7),
            accent = Color(0xFF00E5FF), // Vibranium Neon
            border = Color(0xFF4A148C),
            fontFamilyLabel = "Compact"
        ),
        Skin(
            id = "doctor_strange",
            name = "Doctor Strange",
            category = "Marvel",
            primary = Color(0xFFB71C1C), // Cloak Red
            secondary = Color(0xFFFF6D00), // Mystic Orange
            background = Color(0xFF0E0712),
            surface = Color(0xFF1F122B),
            onSurface = Color(0xFFFFE0B2),
            accent = Color(0xFFFFD700), // Spell Gold
            border = Color(0xFFFF6D00),
            fontFamilyLabel = "Normal"
        ),
        Skin(
            id = "hulk",
            name = "Hulk",
            category = "Marvel",
            primary = Color(0xFF2E7D32), // Gamma Green
            secondary = Color(0xFF6A1B9A), // Torn Pants Purple
            background = Color(0xFF0D160E),
            surface = Color(0xFF1D2F1F),
            onSurface = Color(0xFFE8F5E9),
            accent = Color(0xFF76FF03),
            border = Color(0xFF6A1B9A),
            fontFamilyLabel = "Blocky"
        ),
        Skin(
            id = "deadpool",
            name = "Deadpool",
            category = "Marvel",
            primary = Color(0xFFC62828), // Suit Red
            secondary = Color(0xFF212121), // Slate Black
            background = Color(0xFF1C1313),
            surface = Color(0xFF2E1F1F),
            onSurface = Color(0xFFECEFF1),
            accent = Color(0xFFFFD54F), // Chimichanga Yellow
            border = Color(0xFF212121),
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "wolverine",
            name = "Wolverine",
            category = "Marvel",
            primary = Color(0xFFFBC02D), // Mutant Yellow
            secondary = Color(0xFF0D47A1), // Classic Blue
            background = Color(0xFF17130A),
            surface = Color(0xFF2E2719),
            onSurface = Color(0xFFFFF9C4),
            accent = Color(0xFF90A4AE), // Adamantium Silver
            border = Color(0xFFFBC02D),
            fontFamilyLabel = "Blocky"
        ),
        Skin(
            id = "thanos",
            name = "Thanos",
            category = "Marvel",
            primary = Color(0xFF6A1B9A), // Thanos Purple
            secondary = Color(0xFFFFD54F), // Gauntlet Gold
            background = Color(0xFF130922),
            surface = Color(0xFF27173D),
            onSurface = Color(0xFFF3E5F5),
            accent = Color(0xFF00E5FF), // Mind Stone Teal
            border = Color(0xFFFFD54F),
            fontFamilyLabel = "Hero"
        ),
        Skin(
            id = "scarlet_witch",
            name = "Scarlet Witch",
            category = "Marvel",
            primary = Color(0xFF880E4F), // Scarlet Maroon
            secondary = Color(0xFFE91E63), // Chaos Red
            background = Color(0xFF13030A),
            surface = Color(0xFF2C0B1B),
            onSurface = Color(0xFFFCE4EC),
            accent = Color(0xFFFF4081),
            border = Color(0xFFE91E63),
            fontFamilyLabel = "Normal"
        ),
        Skin(
            id = "loki",
            name = "Loki",
            category = "Marvel",
            primary = Color(0xFF1B5E20), // Trickster Green
            secondary = Color(0xFFFFD700), // Horned Gold
            background = Color(0xFF09140B),
            surface = Color(0xFF182D1B),
            onSurface = Color(0xFFE8F5E9),
            accent = Color(0xFFFFEB3B),
            border = Color(0xFF1B5E20),
            fontFamilyLabel = "Normal"
        ),
        Skin(
            id = "venom",
            name = "Venom",
            category = "Marvel",
            primary = Color(0xFF000000), // Midnight Black
            secondary = Color(0xFFFFFFFF), // Creepy White
            background = Color(0xFF0A0A0A),
            surface = Color(0xFF181818),
            onSurface = Color(0xFFECEFF1),
            accent = Color(0xFFD50000), // Symbiote Tongue Red
            border = Color(0xFF333333),
            fontFamilyLabel = "Blocky"
        ),
        Skin(
            id = "star_lord",
            name = "Star-Lord",
            category = "Marvel",
            primary = Color(0xFF8D6E63), // Jacket Leather
            secondary = Color(0xFF00ACC1), // Space Teal
            background = Color(0xFF151216),
            surface = Color(0xFF261E24),
            onSurface = Color(0xFFE0F7FA),
            accent = Color(0xFFFF1744), // Helmet Eye Red
            border = Color(0xFF00ACC1),
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "hawkeye",
            name = "Hawkeye",
            category = "Marvel",
            primary = Color(0xFF4A148C), // Quiver Purple
            secondary = Color(0xFF1A1A1A), // Matte Black
            background = Color(0xFF110B18),
            surface = Color(0xFF22172F),
            onSurface = Color(0xFFE8EAF6),
            accent = Color(0xFFFF9100), // Arrow Orange
            border = Color(0xFF4A148C),
            fontFamilyLabel = "Normal"
        ),

        // === DC UNIVERSE (15 Skins) ===
        Skin(
            id = "batman",
            name = "Batman",
            category = "DC",
            primary = Color(0xFF212121), // Charcoal Noir
            secondary = Color(0xFFFBC02D), // Bat Signal Gold
            background = Color(0xFF0D0D0D),
            surface = Color(0xFF1A1A1A),
            onSurface = Color(0xFFEEEEEE),
            accent = Color(0xFF757575),
            border = Color(0xFFFBC02D),
            fontFamilyLabel = "Hero"
        ),
        Skin(
            id = "superman",
            name = "Superman",
            category = "DC",
            primary = Color(0xFF0D47A1), // Metropolis Blue
            secondary = Color(0xFFD32F2F), // Cape Red
            background = Color(0xFF080D1A),
            surface = Color(0xFF16213E),
            onSurface = Color(0xFFE3F2FD),
            accent = Color(0xFFFFEB3B), // S-Shield Gold
            border = Color(0xFF0D47A1),
            fontFamilyLabel = "Hero"
        ),
        Skin(
            id = "wonder_woman",
            name = "Wonder Woman",
            category = "DC",
            primary = Color(0xFFFFD700), // Amazonian Gold
            secondary = Color(0xFFB71C1C), // Star Spangled Crimson
            background = Color(0xFF170C0D),
            surface = Color(0xFF331416),
            onSurface = Color(0xFFFFF7C2),
            accent = Color(0xFF0D47A1), // Tiara Blue
            border = Color(0xFFFFD700),
            fontFamilyLabel = "Hero"
        ),
        Skin(
            id = "the_flash",
            name = "The Flash",
            category = "DC",
            primary = Color(0xFFD50000), // Speedforce Scarlet
            secondary = Color(0xFFFFEA00), // Lightning Yellow
            background = Color(0xFF1E0606),
            surface = Color(0xFF3B1212),
            onSurface = Color(0xFFFFFDE7),
            accent = Color(0xFFFFEA00),
            border = Color(0xFFFFEA00),
            fontFamilyLabel = "Compact"
        ),
        Skin(
            id = "joker",
            name = "Joker",
            category = "DC",
            primary = Color(0xFF4A148C), // Arkham Purple
            secondary = Color(0xFF2E7D32), // Acid Green
            background = Color(0xFF140D1B),
            surface = Color(0xFF271A35),
            onSurface = Color(0xFFE8F5E9),
            accent = Color(0xFFD50000), // HAHA Red
            border = Color(0xFF2E7D32),
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "harley_quinn",
            name = "Harley Quinn",
            category = "DC",
            primary = Color(0xFFC2185B), // Jester Pink/Red
            secondary = Color(0xFF1976D2), // Jester Blue
            background = Color(0xFF1F1117),
            surface = Color(0xFF381C28),
            onSurface = Color(0xFFECEFF1),
            accent = Color(0xFFFFFFFF),
            border = Color(0xFFC2185B),
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "aquaman",
            name = "Aquaman",
            category = "DC",
            primary = Color(0xFFEF6C00), // Atlantis Orange
            secondary = Color(0xFF2E7D32), // Deep Sea Green
            background = Color(0xFF081412),
            surface = Color(0xFF122C27),
            onSurface = Color(0xFFE0F2F1),
            accent = Color(0xFFFFD54F), // Trident Gold
            border = Color(0xFFEF6C00),
            fontFamilyLabel = "Normal"
        ),
        Skin(
            id = "green_lantern",
            name = "Green Lantern",
            category = "DC",
            primary = Color(0xFF1B5E20), // Willpower Green
            secondary = Color(0xFF00E676), // Bright Construct Emerald
            background = Color(0xFF051307),
            surface = Color(0xFF122E17),
            onSurface = Color(0xFFE8F5E9),
            accent = Color(0xFF00E676),
            border = Color(0xFF00E676),
            fontFamilyLabel = "Hero"
        ),
        Skin(
            id = "shazam",
            name = "Shazam",
            category = "DC",
            primary = Color(0xFFD50000), // Shazam Red
            secondary = Color(0xFFFFEB3B), // Bolt Yellow
            background = Color(0xFF190909),
            surface = Color(0xFF331616),
            onSurface = Color(0xFFFFFD00),
            accent = Color(0xFFFFFFFF), // Cape White
            border = Color(0xFFFFEB3B),
            fontFamilyLabel = "Blocky"
        ),
        Skin(
            id = "robin",
            name = "Robin",
            category = "DC",
            primary = Color(0xFFC62828), // Robin Red
            secondary = Color(0xFF2E7D32), // Robin Green
            background = Color(0xFF11140E),
            surface = Color(0xFF20271A),
            onSurface = Color(0xFFFFF9C4),
            accent = Color(0xFFFFEB3B), // Utility Yellow
            border = Color(0xFF2E7D32),
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "nightwing",
            name = "Nightwing",
            category = "DC",
            primary = Color(0xFF00B0FF), // Blüdhaven Cyan Blue
            secondary = Color(0xFF1A1A1A), // Midnight Black
            background = Color(0xFF0C1017),
            surface = Color(0xFF171E28),
            onSurface = Color(0xFFE0F7FA),
            accent = Color(0xFF00E5FF),
            border = Color(0xFF00B0FF),
            fontFamilyLabel = "Compact"
        ),
        Skin(
            id = "riddler",
            name = "Riddler",
            category = "DC",
            primary = Color(0xFF2E7D32), // Question Green
            secondary = Color(0xFF000000), // Question Black
            background = Color(0xFF0D140E),
            surface = Color(0xFF1C2D1F),
            onSurface = Color(0xFFA5D6A7),
            accent = Color(0xFF76FF03), // Riddle Neon
            border = Color(0xFF2E7D32),
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "cyborg",
            name = "Cyborg",
            category = "DC",
            primary = Color(0xFF9E9E9E), // Chrome Silver
            secondary = Color(0xFF00B0FF), // Neon Blue Eye
            background = Color(0xFF12151B),
            surface = Color(0xFF222831),
            onSurface = Color(0xFFECEFF1),
            accent = Color(0xFFFF1744), // Charging Red
            border = Color(0xFF9E9E9E),
            fontFamilyLabel = "Blocky"
        ),
        Skin(
            id = "supergirl",
            name = "Supergirl",
            category = "DC",
            primary = Color(0xFF1565C0), // Hope Blue
            secondary = Color(0xFFC62828), // Cape Red
            background = Color(0xFF0C121F),
            surface = Color(0xFF1D283F),
            onSurface = Color(0xFFE3F2FD),
            accent = Color(0xFFFFEB3B),
            border = Color(0xFF1565C0),
            fontFamilyLabel = "Hero"
        ),
        Skin(
            id = "lex_luthor",
            name = "Lex Luthor",
            category = "DC",
            primary = Color(0xFF6A1B9A), // LexCorp Purple
            secondary = Color(0xFF2E7D32), // Kryptonite Green
            background = Color(0xFF130E1A),
            surface = Color(0xFF241C31),
            onSurface = Color(0xFFE1BEE7),
            accent = Color(0xFF00E676),
            border = Color(0xFF6A1B9A),
            fontFamilyLabel = "Normal"
        ),

        // === CLASSIC & MODERN CARTOONS (20 Skins) ===
        Skin(
            id = "ben_10",
            name = "Ben 10",
            category = "Cartoons",
            primary = Color(0xFF76FF03), // Omnitrix Lime
            secondary = Color(0xFF000000), // Matte Black
            background = Color(0xFF0A1208),
            surface = Color(0xFF1B2C18),
            onSurface = Color(0xFFCCFF90),
            accent = Color(0xFFFFFFFF),
            border = Color(0xFF76FF03),
            fontFamilyLabel = "Blocky"
        ),
        Skin(
            id = "dragon_ball_z",
            name = "Dragon Ball Z",
            category = "Cartoons",
            primary = Color(0xFFFF6D00), // Goku Saiyan Orange
            secondary = Color(0xFF1A237E), // Saiyan Blue
            background = Color(0xFF1C130D),
            surface = Color(0xFF332014),
            onSurface = Color(0xFFFFCC80),
            accent = Color(0xFFFFD600), // Super Saiyan Hair Gold
            border = Color(0xFFFF6D00),
            fontFamilyLabel = "Blocky"
        ),
        Skin(
            id = "naruto",
            name = "Naruto",
            category = "Cartoons",
            primary = Color(0xFFFF5722), // Hidden Leaf Orange
            secondary = Color(0xFF212121), // Shinobi Black
            background = Color(0xFF18100E),
            surface = Color(0xFF301C17),
            onSurface = Color(0xFFFFCCBC),
            accent = Color(0xFF00B0FF), // Rasengan Blue
            border = Color(0xFFFF5722),
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "pokemon",
            name = "Pokémon",
            category = "Cartoons",
            primary = Color(0xFFFFEA00), // Pikachu Yellow
            secondary = Color(0xFFD50000), // Pokéball Red
            background = Color(0xFF1E1C0F),
            surface = Color(0xFF3B361B),
            onSurface = Color(0xFFFFFDE7),
            accent = Color(0xFF2979FF), // Ash Ketchup Indigo Blue
            border = Color(0xFFFFEA00),
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "spongebob",
            name = "SpongeBob",
            category = "Cartoons",
            primary = Color(0xFFFFEB3B), // Sponge Yellow
            secondary = Color(0xFF795548), // Pants Brown
            background = Color(0xFFE0F7FA), // ocean Teal (Light theme feel!)
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF3E2723),
            accent = Color(0xFF03A9F4),
            border = Color(0xFFFFC107),
            isDark = false,
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "tom_and_jerry",
            name = "Tom and Jerry",
            category = "Cartoons",
            primary = Color(0xFF78909C), // Tom Slate Gray
            secondary = Color(0xFF8D6E63), // Jerry Soft Brown
            background = Color(0xFFECEFF1), // Cozy Home Cream
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF263238),
            accent = Color(0xFFFFC107), // Cheese Yellow
            border = Color(0xFF78909C),
            isDark = false,
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "scooby_doo",
            name = "Scooby-Doo",
            category = "Cartoons",
            primary = Color(0xFF8D6E63), // Scooby Brown
            secondary = Color(0xFF00ACC1), // Mystery Machine Aqua
            background = Color(0xFF151C1E),
            surface = Color(0xFF1E2F33),
            onSurface = Color(0xFFE0F7FA),
            accent = Color(0xFF76FF03), // Shabby Lime Green
            border = Color(0xFF00ACC1),
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "samurai_jack",
            name = "Samurai Jack",
            category = "Cartoons",
            primary = Color(0xFFFFFFFF), // Kimono White
            secondary = Color(0xFFB71C1C), // Aku Crimson
            background = Color(0xFF050505),
            surface = Color(0xFF1A1A1A),
            onSurface = Color(0xFFEEEEEE),
            accent = Color(0xFF00E5FF), // Magic Sword Glow
            border = Color(0xFFB71C1C),
            fontFamilyLabel = "Compact"
        ),
        Skin(
            id = "dexters_lab",
            name = "Dexter's Lab",
            category = "Cartoons",
            primary = Color(0xFFECEFF1), // Lab Coat White
            secondary = Color(0xFF1DE9B6), // Lab Flask Mint Green
            background = Color(0xFF101712),
            surface = Color(0xFF1D2F22),
            onSurface = Color(0xFFCCFF90),
            accent = Color(0xFFFF3D00), // Dexter Hair Orange
            border = Color(0xFF1DE9B6),
            fontFamilyLabel = "Mono"
        ),
        Skin(
            id = "courage",
            name = "Courage",
            category = "Cartoons",
            primary = Color(0xFFEC407A), // Scared Soft Pink
            secondary = Color(0xFF3F51B5), // Shadow Indigo
            background = Color(0xFF1F101A),
            surface = Color(0xFF371D2D),
            onSurface = Color(0xFFF8BBD0),
            accent = Color(0xFFFFEB3B), // Windmill Light Gold
            border = Color(0xFFEC407A),
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "powerpuff_girls",
            name = "Powerpuff Girls",
            category = "Cartoons",
            primary = Color(0xFFF48FB1), // Blossom Pink
            secondary = Color(0xFF81C784), // Buttercup Green
            background = Color(0xFF0C0F1A),
            surface = Color(0xFF1C2037),
            onSurface = Color(0xFFE1BEE7),
            accent = Color(0xFF29B6F6), // Bubbles Blue
            border = Color(0xFFF48FB1),
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "phineas_and_ferb",
            name = "Phineas & Ferb",
            category = "Cartoons",
            primary = Color(0xFFFF9800), // Phineas Shirt Orange
            secondary = Color(0xFF00E5FF), // Perry Teal
            background = Color(0xFF121A1D),
            surface = Color(0xFF203137),
            onSurface = Color(0xFFE0F7FA),
            accent = Color(0xFFE040FB), // Candace Pink
            border = Color(0xFF00E5FF),
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "avatar_airbender",
            name = "Avatar: Airbender",
            category = "Cartoons",
            primary = Color(0xFFFF8F00), // Air Nomad Orange
            secondary = Color(0xFFFFEB3B), // Arrow Tattoo Cyan Yellow
            background = Color(0xFF0E141D),
            surface = Color(0xFF1B2636),
            onSurface = Color(0xFFE0F7FA),
            accent = Color(0xFF00E5FF), // Glider Air Arrow Glow
            border = Color(0xFFFF8F00),
            fontFamilyLabel = "Normal"
        ),
        Skin(
            id = "rick_and_morty",
            name = "Rick and Morty",
            category = "Cartoons",
            primary = Color(0xFF00E676), // Portal Gate Lime Green
            secondary = Color(0xFF4DD0E1), // Rick Shirt Soft Blue
            background = Color(0xFF0B1413),
            surface = Color(0xFF1C302B),
            onSurface = Color(0xFFB2DFDB),
            accent = Color(0xFFE040FB), // Toxic Pink Hue
            border = Color(0xFF00E676),
            fontFamilyLabel = "Mono"
        ),
        Skin(
            id = "adventure_time",
            name = "Adventure Time",
            category = "Cartoons",
            primary = Color(0xFF4FC3F7), // Finn Hood Blue
            secondary = Color(0xFFFFB300), // Jake Gold Yellow
            background = Color(0xFF0F1B22),
            surface = Color(0xFF1F3543),
            onSurface = Color(0xFFE0F7FA),
            accent = Color(0xFF81C784), // Grass Sword Green
            border = Color(0xFFFFB300),
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "teen_titans",
            name = "Teen Titans",
            category = "Cartoons",
            primary = Color(0xFF78909C), // Tower Slate Gray
            secondary = Color(0xFF00E5FF), // T-Communicator Cyan
            background = Color(0xFF0F1115),
            surface = Color(0xFF222831),
            onSurface = Color(0xFFE0F7FA),
            accent = Color(0xFFFF1744), // Robin Cape Scarlet
            border = Color(0xFF00E5FF),
            fontFamilyLabel = "Compact"
        ),
        Skin(
            id = "danny_phantom",
            name = "Danny Phantom",
            category = "Cartoons",
            primary = Color(0xFF00E676), // Ghost Zone Green
            secondary = Color(0xFF3F51B5), // Hazmat Suit Indigo
            background = Color(0xFF050811),
            surface = Color(0xFF11192E),
            onSurface = Color(0xFFE8F5E9),
            accent = Color(0xFFFFFFFF), // Portal White Glare
            border = Color(0xFF00E676),
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "gravity_falls",
            name = "Gravity Falls",
            category = "Cartoons",
            primary = Color(0xFF2E7D32), // Pines Pine Green
            secondary = Color(0xFF8D6E63), // Mystery Shack Wood Brown
            background = Color(0xFF0E130E),
            surface = Color(0xFF1E2E1E),
            onSurface = Color(0xFFCFD8DC),
            accent = Color(0xFFFFD54F), // Bill Cipher Gold Triangle
            border = Color(0xFF2E7D32),
            fontFamilyLabel = "Comic"
        ),
        Skin(
            id = "johnny_bravo",
            name = "Johnny Bravo",
            category = "Cartoons",
            primary = Color(0xFFFBC02D), // Legendary Blonde Hair Yellow
            secondary = Color(0xFF000000), // Macho Sunglasses Black
            background = Color(0xFF1C130D),
            surface = Color(0xFF2E2218),
            onSurface = Color(0xFFFFE082),
            accent = Color(0xFFE040FB), // Mirror Pink Glam
            border = Color(0xFFFBC02D),
            fontFamilyLabel = "Blocky"
        ),
        Skin(
            id = "popeye",
            name = "Popeye",
            category = "Cartoons",
            primary = Color(0xFF1A237E), // Navy Uniform Blue
            secondary = Color(0xFFFFEB3B), // Anchor Yellow Glow
            background = Color(0xFF060D1A),
            surface = Color(0xFF131E33),
            onSurface = Color(0xFFFFFDE7),
            accent = Color(0xFF4CAF50), // Spinach Can Green
            border = Color(0xFF1A237E),
            fontFamilyLabel = "Normal"
        )
    )

    fun getSkinOrDefault(id: String?): Skin {
        return skins.find { it.id == id } ?: skins.first()
    }
}
