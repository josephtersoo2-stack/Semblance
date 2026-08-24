package app.semblance.data.seed

import app.semblance.data.local.entity.ProfileEntity
import app.semblance.data.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileSeeder @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend fun seedIfEmpty() {
        if (profileRepository.getCount() == 0) {
            val seeded = listOf(
                ProfileEntity(
                    id = 1, suffix = "p1", alias = "alex_prime", age = 26, tz = "America/New_York",
                    voice = "casual tech enthusiast, short sentences", activeHoursStart = 9, activeHoursEnd = 23,
                    commentRate = 0.08f, sessionsPerDay = 5, deviceModel = "Pixel 7 Pro",
                    androidVersion = 14, chromeVersion = 124, screenWidth = 1440, screenHeight = 3120,
                    screenDensity = 3.5f, gpu = "Mali-G710", cores = 8, ramGb = 12, tlsId = "HelloChrome_124",
                    userAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                    clientHintsPlatform = "Android", clientHintsPlatformVersion = "14.0.0", clientHintsModel = "Pixel 7 Pro",
                    proxyType = "http", proxyHost = "res-us.proxy-hub.io", proxyPort = 8001, proxyOk = true,
                    interestsJson = """{"technology":0.45,"ai_research":0.3,"cybersec":0.25}""",
                    lastUrl = "https://news.ycombinator.com", status = "WATCHING", warmth = 82, phase = "ACTIVE"
                ),
                ProfileEntity(
                    id = 2, suffix = "p2", alias = "sarah_travels", age = 29, tz = "America/Los_Angeles",
                    voice = "enthusiastic traveler, friendly lowercase", activeHoursStart = 7, activeHoursEnd = 22,
                    commentRate = 0.12f, sessionsPerDay = 4, deviceModel = "Galaxy S23",
                    androidVersion = 14, chromeVersion = 123, screenWidth = 1080, screenHeight = 2340,
                    screenDensity = 3.0f, gpu = "Adreno 740", cores = 8, ramGb = 8, tlsId = "HelloChrome_123",
                    userAgent = "Mozilla/5.0 (Linux; Android 14; SM-S911B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36",
                    clientHintsPlatform = "Android", clientHintsPlatformVersion = "14.0.0", clientHintsModel = "SM-S911B",
                    proxyType = "http", proxyHost = "res-west.brightdata-net.com", proxyPort = 8002, proxyOk = true,
                    interestsJson = """{"travel":0.5,"japan":0.3,"photography":0.2}""",
                    lastUrl = "https://reddit.com/r/travel", status = "BROWSING", warmth = 68, phase = "ACTIVE"
                ),
                ProfileEntity(
                    id = 3, suffix = "p3", alias = "marcus_dev", age = 22, tz = "Europe/London",
                    voice = "analytical, concise, code references", activeHoursStart = 10, activeHoursEnd = 2,
                    commentRate = 0.04f, sessionsPerDay = 6, deviceModel = "Pixel 6a",
                    androidVersion = 14, chromeVersion = 124, screenWidth = 1080, screenHeight = 2400,
                    screenDensity = 2.625f, gpu = "Mali-G78", cores = 8, ramGb = 6, tlsId = "HelloChrome_124",
                    userAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 6a) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                    clientHintsPlatform = "Android", clientHintsPlatformVersion = "14.0.0", clientHintsModel = "Pixel 6a",
                    proxyType = "http", proxyHost = "res-uk.smartproxy.io", proxyPort = 8003, proxyOk = true,
                    interestsJson = """{"kotlin":0.4,"rust":0.35,"open_source":0.25}""",
                    lastUrl = "https://github.com/trending", status = "TYPING", warmth = 91, phase = "ACTIVE"
                ),
                ProfileEntity(
                    id = 4, suffix = "p4", alias = "elena_vibe", age = 24, tz = "America/Chicago",
                    voice = "artistic, expressive, lowercase", activeHoursStart = 12, activeHoursEnd = 1,
                    commentRate = 0.15f, sessionsPerDay = 3, deviceModel = "OnePlus 11",
                    androidVersion = 13, chromeVersion = 122, screenWidth = 1440, screenHeight = 3216,
                    screenDensity = 3.5f, gpu = "Adreno 740", cores = 8, ramGb = 16, tlsId = "HelloChrome_122",
                    userAgent = "Mozilla/5.0 (Linux; Android 13; CPH2449) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36",
                    clientHintsPlatform = "Android", clientHintsPlatformVersion = "13.0.0", clientHintsModel = "CPH2449",
                    proxyType = "http", proxyHost = "res-midwest.oxylabs.io", proxyPort = 8004, proxyOk = true,
                    interestsJson = """{"design":0.4,"indie_music":0.4,"architecture":0.2}""",
                    lastUrl = "https://dribbble.com", status = "IDLE", warmth = 44, phase = "WARMUP"
                ),
                ProfileEntity(
                    id = 5, suffix = "p5", alias = "jake_gamer", age = 20, tz = "America/New_York",
                    voice = "gaming slang, fast reactions, hype", activeHoursStart = 15, activeHoursEnd = 4,
                    commentRate = 0.20f, sessionsPerDay = 5, deviceModel = "Xiaomi 13",
                    androidVersion = 14, chromeVersion = 124, screenWidth = 1080, screenHeight = 2400,
                    screenDensity = 3.0f, gpu = "Adreno 740", cores = 8, ramGb = 12, tlsId = "HelloChrome_124",
                    userAgent = "Mozilla/5.0 (Linux; Android 14; 2211133C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                    clientHintsPlatform = "Android", clientHintsPlatformVersion = "14.0.0", clientHintsModel = "2211133C",
                    proxyType = "http", proxyHost = "res-us.soax.com", proxyPort = 8005, proxyOk = true,
                    interestsJson = """{"valorant":0.5,"twitch":0.3,"hardware":0.2}""",
                    lastUrl = "https://twitch.tv/directory", status = "WATCHING", warmth = 78, phase = "ACTIVE"
                ),
                ProfileEntity(
                    id = 6, suffix = "p6", alias = "chloe_foodie", age = 31, tz = "Europe/Paris",
                    voice = "culinary enthusiast, thoughtful descriptions", activeHoursStart = 8, activeHoursEnd = 22,
                    commentRate = 0.09f, sessionsPerDay = 3, deviceModel = "Pixel 8",
                    androidVersion = 14, chromeVersion = 124, screenWidth = 1080, screenHeight = 2400,
                    screenDensity = 3.0f, gpu = "Mali-G715", cores = 8, ramGb = 8, tlsId = "HelloChrome_124",
                    userAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                    clientHintsPlatform = "Android", clientHintsPlatformVersion = "14.0.0", clientHintsModel = "Pixel 8",
                    proxyType = "http", proxyHost = "res-fr.iproyal.com", proxyPort = 8006, proxyOk = true,
                    interestsJson = """{"pastry":0.4,"french_cuisine":0.4,"wine":0.2}""",
                    lastUrl = "https://seriouseats.com", status = "SLEEPING", warmth = 32, phase = "WARMUP"
                ),
                ProfileEntity(
                    id = 7, suffix = "p7", alias = "kev_19", age = 19, tz = "America/Chicago",
                    voice = "lowercase, typos ok, no emojis", activeHoursStart = 16, activeHoursEnd = 1,
                    commentRate = 0.05f, sessionsPerDay = 4, deviceModel = "Pixel 6a",
                    androidVersion = 14, chromeVersion = 124, screenWidth = 1080, screenHeight = 2400,
                    screenDensity = 2.625f, gpu = "Adreno 730", cores = 8, ramGb = 8, tlsId = "HelloChrome_124",
                    userAgent = "Mozilla/5.0 (Linux; Android 14; Pixel 6a) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
                    clientHintsPlatform = "Android", clientHintsPlatformVersion = "14.0.0", clientHintsModel = "Pixel 6a",
                    proxyType = "http", proxyHost = "res-central.stormproxies.com", proxyPort = 8007, proxyOk = true,
                    interestsJson = """{"off_road_trucks":0.4,"gaming":0.3,"music":0.2}""",
                    lastUrl = "https://youtube.com/results?search_query=ford+raptor+baja", status = "WAKING", warmth = 12, phase = "WARMUP"
                ),
                ProfileEntity(
                    id = 8, suffix = "p8", alias = "nova_stream", age = 27, tz = "America/Denver",
                    voice = "science geek, curious questions", activeHoursStart = 8, activeHoursEnd = 23,
                    commentRate = 0.07f, sessionsPerDay = 4, deviceModel = "Galaxy S22",
                    androidVersion = 14, chromeVersion = 123, screenWidth = 1080, screenHeight = 2340,
                    screenDensity = 3.0f, gpu = "Xclipse 920", cores = 8, ramGb = 8, tlsId = "HelloChrome_123",
                    userAgent = "Mozilla/5.0 (Linux; Android 14; SM-S901B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36",
                    clientHintsPlatform = "Android", clientHintsPlatformVersion = "14.0.0", clientHintsModel = "SM-S901B",
                    proxyType = "http", proxyHost = "res-co.proxyscrape.com", proxyPort = 8008, proxyOk = true,
                    interestsJson = """{"astronomy":0.5,"james_webb":0.3,"quantum":0.2}""",
                    lastUrl = "https://wikipedia.org/wiki/James_Webb_Space_Telescope", status = "BROWSING", warmth = 55, phase = "ACTIVE"
                )
            )
            profileRepository.saveAll(seeded)
        }
    }
}
