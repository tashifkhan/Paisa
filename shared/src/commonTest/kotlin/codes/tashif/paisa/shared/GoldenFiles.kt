package codes.tashif.paisa.shared

import kotlin.io.encoding.Base64

/**
 * Files written by the Android app's own code, so the shared code has to
 * read what phones really upload.
 */
object GoldenFiles {
    /**
     * A full sync change file from `SyncEngine.export` on a fresh database
     * plus one account, "Golden Bank". Sealed by `BackupCrypto` with
     * [SYNC_PASSWORD] at 210,000 iterations.
     */
    val syncFull: ByteArray = Base64.decode(
        "UEFJU0FCQUsBAfdlyh9qS6sRHxoJxXHkS1EAAzRQBU3g9ECDjTnnhEE6b2EjwcwmKRpktA0WLS3jpsCALM0FZiFXFe1VMzECv4ZMt95ZnjqcWwAE8eaW30pBL2jqGs2UqaKXUmUPIpA59cv5nTvlIajTRHdUhwGPVXRicpYtPOiCIZ1hEcYNiZAJkZPngH3vy0DlbcmizN/27mGy3U3bw9mKCGmCzzXejAZlcJdOpItKxPx0WnnAWxaYKTcjl0f4U06gR/OvpUUgifX/Fzs7NL7huDzPOSN52EsGo50AXQ18njX3RFmEle6k0ka/Om0H/yCnIqV39EmALcZ6zRaJq+52Mc26vaBfnKXO1kKXp0vioPnoGaS3f+Owdlh08UsI+PFHCH6ETtSt2XCOkicq/kQ5mDK53SQwzYVhHnmO6CjauUsJF+ZWyySjRMFEx0p41IEQj8a9QyAwOhNPT0wAVP1yJEnKtaCp6FD/CQWwLIEGSWO7fBG/emk6IS+WrGnQ1nsUK786uyBt71oQlJM7zTcg9qwkAAwUMY5mL6n1EDGsvx5Y15XI0dVFw/gzg76jDXg6HeZiWM0pKSj/C89hh4Js0neU5/0YRv97KITnvlsZZA6xM7CUPPAgp0Ez72gJm4uKbFdloYUdNVMG1HXe1ns3wI0G3aSoG1Gxv1a7kHAjn6aJu7o4ynJIoTvQDLMLWR3EovtbZjZWq3rRIiamu3eshXp0+parH1Qoh6P7ZHInwS9ZWPHdfgUxLsfNJh/htApS+3luFeQPADoXCQjhcBkY/IeAHeS2TpvJZjQngLMkZ7Q4MWVrUSpZm/HoHktn6FyzAzEpd4G/ik3Ky+KdUPbnzbMnePblooNqWYAxG7okKp85UncWcUglj/MGdO3AdkdpT7AWONzid7YCu7eLImLsMDZlHK6tHHMWaeS4LuXTQahBWSXmlRBn33+ZEfPFP+VOGu78mAYXpN8EkP3gr5LZc2XVDOjn6Ce8MruFNWr+5Ogv3t4Siu0LXOBzeU8P/v63MbweT5FylxOvRZ9Y51IU1YJZV+uFXFJ1DE6E0nOFVXJGuR+Kt2OLeJG/YabrPz8MoiJizlLLscx79ezdfXg1g9lmtFuspez7rCzsuKJ43Tw045GvXnJFQ41nI41KfoDHZIMHhiqVJAI1uy1FrKD4QqWbxALQqIGnDlMqKvE98ZQl1LisJ/R5NZcTPljoyfnykT3MYsMXW1o15Vhu2RLzRP//Ylqwub8DDWahP6matOAIAEU6bF3evgBs9pfsvG8GCiEriKuyDl/2eKsKA+deNG5Zwf5sRQAH4Ainsk3AZ1ChAldostgbXxEgvJCZWUfiRGaX+0VAnke9pAM1XNAAsGmytypguMDSY/DQMakBlAq3ZzeFGpkSUlkq1pe2RTUnSxpLbmmcPNJ0nvBZNLhLSbenx5pEzZMQ1uu3QAOvSAd7mo+x1PTFkww0vjqfnCO7vF5gmqlaqAJZ6EqK/yVdHOiZ4eLq0DkyTcJkWmDwsSRcKa7HNH425Cd8YlsKjdpsq2JCY0v6b401dKSHAX1oG9Y9cblU5CKddqGdqxXNkxpsW6OIddkN/z4Ta2mkqd8rqtUqb8DqytcJk3DNZpsJZrdyzkWi7m9MbmnJr/fmy1YAAs66iD3R3gbJgEDUY7vUodOil/cw/fhcOIZvYJCtL5CPUBgnKweWlV42izKIt3LueMIXhEXLXKEv/plqWN2AjnPEUPvuLp/HA9kBljrTfHqvhWuagGamRwQRnXKKFer07ywOy1sSa7UYvqpQFKuSJr+Lsnj5Zyp3jbISNRxw6JP96UBtigVEVk0ZNeeJLXDJRf6npxNi8BdZE/1Z+RL8US2bvsN86ihf5c97oFRuukJOoDpD87tfFA=="
    )

    /** Non-ASCII on purpose: it catches a password encoded as anything but UTF-8. */
    const val SYNC_PASSWORD = "पैसा 🔐 sync"
}
