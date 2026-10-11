package com.echo.inbox.core.util

import com.echo.inbox.core.model.AppCategory

object AppCategoryClassifier {
    private val messagingPackages = listOf(
        "com.whatsapp",
        "org.telegram",
        "org.thoughtcrime.securesms",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.facebook.orca",
        "com.viber.voip",
        "com.discord",
        "com.slack",
        "com.microsoft.teams",
    )
    private val socialPackages = listOf(
        "com.instagram",
        "com.facebook.katana",
        "com.twitter",
        "com.google.android.youtube",
        "com.snapchat",
        "com.reddit",
        "com.linkedin",
        "com.threads",
        "com.pinterest",
    )
    private val workPackages = listOf(
        "com.slack",
        "com.microsoft.teams",
        "com.google.android.apps.chat",
        "com.zoom",
        "com.notion",
        "com.trello",
    )
    private val emailPackages = listOf(
        "com.google.android.gm",
        "com.microsoft.office.outlook",
        "com.yahoo.mobile.client.android.mail",
        "com.samsung.android.email",
        "ch.protonmail.android",
    )
    private val shoppingPackages = listOf(
        "com.amazon",
        "com.flipkart",
        "com.myntra",
        "com.swiggy",
        "in.zomato",
        "com.grofers",
        "com.zepto",
        "com.ubercab",
        "com.olacabs",
        "com.rapido",
        "com.dunzo",
        "com.blinkit",
        "com.meesho",
        "com.ajio",
    )
    private val sensitiveTerms = listOf(
        "bank",
        "wallet",
        "pay",
        "phonepe",
        "paytm",
        "gpay",
        "authenticator",
        "password",
        "health",
        "medical",
        "finance",
        "broker",
        "trading",
        "upi",
        "cred",
    )

    data class Result(val category: AppCategory, val sensitive: Boolean)

    fun classify(packageName: String, displayName: String): Result {
        val packageLower = packageName.lowercase()
        val nameLower = displayName.lowercase()
        val sensitive = sensitiveTerms.any { it in packageLower || it in nameLower }
        if (sensitive) return Result(AppCategory.SENSITIVE, true)

        val category = when {
            messagingPackages.any(packageLower::startsWith) -> AppCategory.MESSAGING
            socialPackages.any(packageLower::startsWith) -> AppCategory.SOCIAL
            workPackages.any(packageLower::startsWith) -> AppCategory.WORK
            emailPackages.any(packageLower::startsWith) -> AppCategory.EMAIL
            shoppingPackages.any(packageLower::startsWith) -> AppCategory.SHOPPING
            packageLower.startsWith("com.android") || packageLower.startsWith("com.google.android") -> AppCategory.SYSTEM
            else -> AppCategory.OTHER
        }
        return Result(category, false)
    }
}
