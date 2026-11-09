package codes.tashif.paisa.ui.onboarding.pages

import codes.tashif.paisa.ui.onboarding.OnboardingPage
import codes.tashif.paisa.ui.sms.smsImportSupported

// Playful copy + full feature coverage. The SMS page only shows where SMS import works.
internal val onboardingPages: List<OnboardingPage> = listOfNotNull(
    helloPage,
    privacyPage,
    homePage,
    smsPage.takeIf { smsImportSupported },
    statementsPage,
    insightsPage,
    accountsPage,
    categoriesPage,
    youVibesPage,
    readyPage
)
