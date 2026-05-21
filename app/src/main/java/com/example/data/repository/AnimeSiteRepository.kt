package com.example.data.repository

import com.example.data.db.AnimeSiteDao
import com.example.data.model.AnimeSite
import kotlinx.coroutines.flow.Flow

class AnimeSiteRepository(private val animeSiteDao: AnimeSiteDao) {

    val allSites: Flow<List<AnimeSite>> = animeSiteDao.getAllSites()

    suspend fun getSiteById(id: Int): AnimeSite? = animeSiteDao.getSiteById(id)

    suspend fun getSiteByUrl(url: String): AnimeSite? = animeSiteDao.getSiteByUrl(url)

    suspend fun insertSite(site: AnimeSite): Long = animeSiteDao.insertSite(site)

    suspend fun updateSite(site: AnimeSite) = animeSiteDao.updateSite(site)

    suspend fun deleteSiteById(id: Int) = animeSiteDao.deleteSiteById(id)

    suspend fun prepopulateDefaultSites() {
        if (animeSiteDao.getCount() == 0) {
            val defaults = listOf(
                AnimeSite(
                    name = "Tubi TV",
                    url = "https://tubitv.com",
                    type = "legal",
                    status = "online",
                    trustScore = 100,
                    category = "Safe & Legal",
                    sslValid = true,
                    virusTotalFlags = 0,
                    notes = "The hidden goldmine in 2026. 100% free legal streaming with massive official studio licensing deals. Perfect safety, zero redirects."
                ),
                AnimeSite(
                    name = "Pluto TV",
                    url = "https://pluto.tv",
                    type = "legal",
                    status = "online",
                    trustScore = 100,
                    category = "Safe & Legal",
                    sslValid = true,
                    virusTotalFlags = 0,
                    notes = "Offers themed, 24/7 commercial-backed linear live streams (like dedicated Naruto or Yu-Gi-Oh channels) plus free on-demand titles."
                ),
                AnimeSite(
                    name = "Muse Asia",
                    url = "https://www.youtube.com/@MuseAsia",
                    type = "legal",
                    status = "online",
                    trustScore = 100,
                    category = "Safe & Legal",
                    sslValid = true,
                    virusTotalFlags = 0,
                    notes = "Authorized anime licensor streaming complete, subbed episodes legally on YouTube. Safe and lightning fast."
                ),
                AnimeSite(
                    name = "Ani-One Asia",
                    url = "https://www.youtube.com/@AniOneAsia",
                    type = "legal",
                    status = "online",
                    trustScore = 100,
                    category = "Safe & Legal",
                    sslValid = true,
                    virusTotalFlags = 0,
                    notes = "Official media channel broadcasting high-quality licensed anime series direct on YouTube for free. Zero security risks."
                ),
                AnimeSite(
                    name = "Crunchyroll",
                    url = "https://www.crunchyroll.com",
                    type = "legal",
                    status = "online",
                    trustScore = 95,
                    category = "Safe & Legal",
                    sslValid = true,
                    virusTotalFlags = 0,
                    notes = "The undisputed leader. Premium subscriptions are prioritized, but they offer ad-supported plans on select catalog titles. 100% secure."
                ),
                AnimeSite(
                    name = "AnimePahe",
                    url = "https://animepahe.ru",
                    type = "unofficial",
                    status = "online",
                    trustScore = 75,
                    category = "Community Mirror",
                    sslValid = true,
                    virusTotalFlags = 1,
                    notes = "Mobile fan-favorite. Video encodes are compact (saving lots of cellular data) but scale excellently to 1080p. Adblocker like uBlock is highly recommended to stop reroutes."
                ),
                AnimeSite(
                    name = "HiAnime",
                    url = "https://hianime.to",
                    type = "unofficial",
                    status = "online",
                    trustScore = 70,
                    category = "Community Mirror",
                    sslValid = true,
                    virusTotalFlags = 2,
                    notes = "The massive 2026 Zoro.to rebrand. Polished Netflix-style dashboard, active Disqus discussion boards, and huge library, but aggressive popup redirection makes uBlock mandatory."
                ),
                AnimeSite(
                    name = "GogoAnime",
                    url = "https://gogoanime3.co",
                    type = "unofficial",
                    status = "online",
                    trustScore = 60,
                    category = "Community Mirror",
                    sslValid = true,
                    virusTotalFlags = 4,
                    notes = "Oldest survivor of domain crackdowns. Changes endings (e.g. .co, .it, .vc) rapidly. It is safe with uBlock, but be extremely careful of impostor clones on Google."
                )
            )
            animeSiteDao.insertSites(defaults)
        }
    }
}
