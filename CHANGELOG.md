# Changelog

Every Billi release, newest first. Releases up to 1.8.0 shipped as Paisa. Each version's APK is on the [releases page](https://github.com/tashifkhan/Billi/releases) and in the F-Droid repo at `https://tashif.codes/fdroid/repo`.

## 2.0.2

### Web

- [billi.taf.sh](https://billi.taf.sh) is now a landing page with screenshots, the walkthrough video and the download links. The app moved to [billi.taf.sh/app](https://billi.taf.sh/app). Data you already have in the browser stays, since it's the same site.
- The address bar follows the app: `/app/analytics`, `/app/settings/backup`, `/app/transactions/42`. Reload or share a link and it opens on that screen. Pages that don't exist show a proper "not found" page.
- Google Drive shows in Backup again.
- The drawer's add button runs the drawer's full width, and Backup, AI, Categories, Merchant rules, Appearance and About sit under the tabs when the window is tall enough.

### Tablets, foldables and the web

- The side rail shows every tab, Debts included, when the window is tall enough. More stops listing tabs the rail already shows.

### Backup

- The Nextcloud sheet explains each field: where to find the server address, why the web needs a CORS proxy and when to leave it empty, and that the app password is not your login password.

## 2.0.1

### Android

- Predictive back is back. Swiping back on a settings page or a transaction shrinks the page toward the edge you swiped from, so you can see where back goes before you let go. Let go early and the page stays.

### Web

- The web version only shows what works in a browser. SMS import, Rescan SMS, app lock, local AI, the "SMS and alerts" AI service and the automatic backup schedule are hidden there. Transactions synced from a phone still show their SMS and notification labels.
- Google Drive backup is hidden on the web until the site has a Google sign-in client ID. Nextcloud and file backups work as before.
- Text that said "this phone" says "this browser" on the web, and statement import asks you to keep the tab open instead of promising a notification.

## 2.0.0

Paisa is now Billi: Budgeting and Expense Manager. It has a new name, a new icon, and a mascot. Billi is a ginger cat who is also a wallet. Everything Paisa did still works the same way: local-first, SMS parsing on the phone, and local AI when the phone has it.

### New name, new package

- The source moved to [github.com/tashifkhan/Billi](https://github.com/tashifkhan/Billi). Old links to the Paisa repository redirect there.
- The web version is at [billi.taf.sh](https://billi.taf.sh).
- The app is called Billi and its package is `sh.taf.billi`. Android sees this as a different app, so Billi installs next to Paisa instead of replacing it.
- To move your data, save a backup in Paisa, then tap "I have a backup" on Billi's first screen. Billi restores `.paisa` files, and lists Paisa's old backups on Google Drive and Nextcloud next to its own.
- New backups are `.billi` files. On Nextcloud they go to a `Billi` folder. Billi never deletes the old `.paisa` backups when it trims old copies.
- Grant SMS and notification access again in Billi. Android ties those permissions to the app.
- Exports are named `billi_transactions_…`, and the web build loads `billi-web.js`.

### Billi the cat

- The launcher icon is Billi on cream, with a themed (monochrome) icon for Android 13 and later and a matching status bar icon.
- Billi shows up in six moods. Happy on the welcome screen and in About. Curious on the SMS step. Winking when setup is done. Asleep or curious on empty lists. Thinking while AI reads a note or receipt, and on the Local AI screen. Guarding the wallet on the lock screen.
- On a dark theme Billi's face turns cream, so it stays visible.
- The big-screen drawer shows Billi next to the app name.

### Assistant actions and shortcuts

- On Android 16 and later, Billi exposes AppFunctions to add an expense or income and read balances, spending, monthly budgets, and recent transactions. Using these actions with Gemini requires registering the package and bundle version through [Google Connected Apps](https://developer.android.com/ai/appfunctions/create-manage-connected-apps).
- An assistant only picks a category Billi already has. If it names one that doesn't exist, Billi uses its merchant rules and keyword map, the same as for an SMS. Wrong input, like a zero amount, an unknown account or a date in the wrong format, comes back to the assistant as an error it can explain.
- Transactions an assistant adds are labelled Assistant in history and filters, and edit like manual ones.
- Every argument except the amount is optional, dates are real dates rather than text, and type only accepts expense or income. Billi also tells the assistant how its functions fit together, and reports any failure as an error the assistant can explain.
- More → Privacy & security → Gemini and assistants turns this off. While it's off, every call is refused. It is on by default and works even with app lock on.
- Long-press the app icon for Add transaction, Add with AI, Budgets and Spending. Add with AI hides when AI is off, and a pinned copy of it greys out.

### Filters

- Under the search bar there's now a chip for each filter: Date, Type, Category, Account, Amount and Source, the way Gmail and Drive do search filters. Each chip opens just its own picker and shows what you picked, like "Last 7 days" or "Food & Dining +1".
- The Filters chip opens every group at once, each in its own card that shows what it's set to and has a Clear button.
- Type is a connected button group. Categories are a grid of their own colored icons, and Pick all selects every category in view. Accounts are a grouped list with checkboxes.
- Amount has one-tap bands (up to ₹500, ₹500 to ₹2,000, ₹2,000 to ₹10,000, ₹10,000 or more) next to the min and max fields.
- The sheet says how many transactions match as you go, and the button at the bottom always shows the count.
- Analytics uses the same look. Month, Year and Custom are a button group. A custom range has Today, 7, 30 and 90 day buttons, and you can tap the start or end day to change the dates, or its time to change the time.
- Analytics' Income, Expense and Saved cards show the change as a small pill, and the four quick stats (average a day, transactions, biggest expense, against the last period) are a grid of their own tiles. Spending and Income in By category is a button group too.

### Under the hood

- Gradle 9.9.2, Android Gradle plugin 9.5.2, Kotlin 2.4.21, KSP 2.3.14, Compose Multiplatform 1.12.2, compileSdk 37.2, and the latest Ktor (3.6, now on OkHttp 5), kotlinx-datetime, CameraX, Glance, WorkManager, haze and the rest.
- Unit tests run on a Java 21 toolchain, which Robolectric needs to simulate Android 16. Gradle downloads it when it's missing.

### Look

- A new Billi palette, taken from the logo's orange and brown, is the default for new installs. Wallpaper colors are still in Appearance, and a restored backup keeps the palette it had.
- The web build has a Billi favicon and orange theme color.
- More → Appearance → Home sections hides the Credit cards or Debts card on Home. Hiding credit cards also drops them from the balance breakdown. Both stay on by default.

## 1.8.0

Payment app notifications, Gemini Nano on the phone, AI for messages the rules can't read, account links for UPI apps, and merchant renames.

### Payment notifications

- More → Payment notifications reads payment alerts from bank and UPI apps. It ships with 14 bank apps, including HDFC, SBI YONO, ICICI iMobile, Axis, Kotak and Canara ai1, and 7 UPI apps: Google Pay, PhonePe, Paytm, BHIM, CRED, FamApp and MobiKwik. "Add a bank or UPI app" adds any other installed app.
- Only the apps switched on are read. Other apps' notifications are dropped before their text is touched, and turning the feature off disconnects Paisa from notifications.
- Alerts like "₹250 paid to Swiggy", "+₹500", "Arjun sent you ₹500" and "Rs 500 debited" are read. Offers, cashback, reminders, payment requests, failures, OTPs and balance alerts are skipped.
- A payment that arrives as a UPI app alert, a bank SMS and a bank app alert is saved once. When the bank's message comes second, its account and balance are added to the row the app created.
- Notification rows show as Notification in history, filters and details.
- Android 15 hides the text of some alerts from apps like Paisa. Those are skipped, and the bank SMS still brings the payment in.

### Which account paid

- Paisa picks the account for an app's payment by the account digits in the alert, then the app's default account, then your only matching account.
- When it can't tell, it asks "Which account was this?". Pick from the alert, from the transaction, or from the list on the Payment notifications screen. "Always use" remembers the account for that app and moves its other waiting payments.
- Each app's row shows its account. Tap it to change the default or remove account links.
- You can change the account of a transaction that came from a notification.

### Unclear messages

- When the rules can't read an SMS or alert, Payment notifications → Unclear messages chooses who tries: Off, On-device or Cloud.
- On-device uses Gemini Nano and nothing leaves the phone. It only runs while Paisa is open, so a message that arrives in the background waits and Paisa posts "Payment waiting for AI". Tap it, or open Paisa, to read it.
- Cloud uses the new "SMS and alerts" service under AI, works on every phone and in the background, and shows a warning that the message text goes to your provider, is slower and may cost money.
- The model can't invent an amount: it has to appear in the message. Messages it says aren't payments stay out of your history.

### Local AI

- AI → Use local AI runs Gemini Nano on the phone with no key and no network. It reads notes, receipts and statement text, one image at a time. PDFs and voice notes still go to cloud models in the ladder.
- The Local AI screen shows whether the phone supports it, downloads the model, and has a test button.
- Works on Pixel 9 and newer, Galaxy S25 and newer, and recent OnePlus, Xiaomi, OPPO and vivo flagships. On other phones the option is greyed out and explains why.

### Merchant rules

- Rename an imported transaction and turn on "Always rename". Past rows are renamed, and future SMS, notifications and statements use the new name.
- More → Merchant rules lists renames and category rules. Renames sync and back up with the rest of your data.

### Fixes

- Tapping a "New transaction" notification opens that transaction. Before, it only opened the app.
- Android 7 no longer crashes when a scan, import or Excel statement posts a notification or reads a date.

## 1.7.7

A fix for the review step in Add with AI.

### Add with AI

- After the AI drafts transactions, each row's note is the Markdown description field from the add sheet, not a one-line text box. Lists, bold and headings now show while you edit, and longer notes have room.

## 1.7.6

Share files to Paisa, read Excel statements, and switch AI off entirely.

### Add with AI

- Share images, PDFs, CSVs, Excel files or text from another app to "Add with AI". The add sheet opens with the files attached and starts drafting. With the app lock on, it waits until you unlock.
- Long statements are read in parts instead of being cut off after the first 24k characters, so every row makes it in.

### Statement import

- Excel .xlsx workbooks work in statement import and Add with AI. Each sheet is read like a CSV, and date cells keep their dates. Old .xls files ask for .xlsx or CSV instead.

### AI switch

- Use AI, at the top of the AI page, turns off every AI feature. Off, Paisa hides Add with AI, statement import, bill scan, the statement tile on the quick actions widget and the AI cards in setup, and drops out of the share sheet. Your keys and models stay saved for when you turn it back on.

## 1.7.5

The split sheet is rebuilt on stock Material 3 components.

### Bill splitting

- Split with others is one card. The switch sits in a list row at the top, and the editor opens under a divider in the same card.
- People are chips. You is a filter chip, each added person is an input chip with an avatar and a remove icon, and the chips wrap onto new lines.
- The search field is filled and pill shaped. Matches show as list rows, and people you already have in Debts appear as one-tap chips while the field is empty.
- Equal, Amount, % and Items are a segmented button under a Divide by label.
- Every mode shows one list of shares, with an avatar, the name and the amount at the end of the row. Amount and % modes edit the share in a compact field on that row.
- Scan a bill and Choose photo sit side by side. Each bill line has its own card with who-had-it chips, and scanning shows a progress bar.
- The Split a bill and edit split sheets in Debts use the same layout.

## 1.7.4

### Bill splitting

- On an item split, take a photo of the bill or choose one you already have. The camera stays inside Paisa. Pixel photos, including Ultra HDR, are straightened and reduced before they're read, and the lines come back for you to check.
- Take photo and Choose photo sit in the same cards as the rest of the sheet.
- Add an item no longer runs into the Save button.

## 1.7.3

Markdown descriptions, more ways to split a bill, category fixes, and support for more bank messages.

### Transactions

- Write descriptions with Markdown when adding or editing a transaction. The toolbar adds formatting, and Preview shows the rendered note. Task boxes can be checked from the rendered description.
- Editable fields come first on the transaction page. Save and Discard stay at the bottom while you have unsaved changes. Bank details and the original message sit under Details.
- Formatting a description now moves typing into that field, so the transaction title stays unchanged.

### Bill splitting

- Split by percentage as well as equally or by custom amounts. Percentage mode starts with the shares already entered.
- Split by item and assign each line to one person or several. Tax, tip and discounts follow each person's share.
- Scan a bill photo to fill the item list, then review the lines and assign people before saving.

### Categories and notifications

- Pick an emoji or a built-in icon when creating or editing a category.
- The duplicate expense categories "Others" and "Other" become one "Other" category. Existing transactions, budgets, recurring entries and merchant rules move to it. Restoring older backups applies the same correction.
- Status bar notifications use new icons that remain readable at small sizes.

### Bank messages

- Added parsers for Barq, IFIC Bank Bangladesh and Mobily Pay.
- SBI Card bill-due reminders no longer become transactions.

### Fixes and maintenance

- Fixed a crash when app lock replaced a screen that held keyboard focus, including during device PIN authentication.
- Reorganised the app's packages and combined the shared module into the app module. Scheduled workers, widgets and Android entry points keep their existing class names.

## 1.7.2

A floating navbar that sits over your list, a way to add a category without leaving the picker, and motion across the app.

### Floating navbar

- **The list shows behind the bar.** Tabs now draw to the bottom edge of the screen and scroll under the floating bar, instead of stopping above it.
- **Tab titles.** A new toggle in More → Appearance puts a title under every tab. Off, only the selected tab shows its name, as before.
- **Frosted glass.** A second toggle blurs and tints what scrolls behind the bar. Blur needs Android 12 or later. Older phones get a see-through tinted bar.
- The bar picks up a little of your accent colour, and on light themes its shadow does too.
- **It moves.** One pill slides between tabs. The bar gives under your finger and lights up where you touch. Each icon makes its own small move when tapped: Home hops, Budgets spins, More turns over.

### Categories

- **Add a category from the picker.** The category list ends with a New category row. Type in the search box first and it reads Create "your text", with the name filled in. Saving selects the new category.
- The new category takes the transaction's type, expense or income. In budgets it is always an expense category.
- Fixed a gap between the category list and the keyboard. The list now runs down to the keyboard.

### Motion

- **Amounts roll.** The balance, income, expense, credit and debts amounts on Home roll up or down when they change, and fade when you hide or show them.
- **Cards give under a finger.** Home cards, budget cards, appearance toggles, picker rows and the add buttons dip when pressed and spring back.
- **Lists glide.** When you add or remove a budget, a person in Debts, a debt entry or a day of transactions, the rows around it slide into place.
- Opening a transaction or a settings screen on a phone fades in with a small rise. Empty states do the same.
- Balance and credit card breakdowns open on a spring, and their arrow turns over.
- Budget bars fill from empty when they appear.

The navbar and press motion is adapted from [Expressive Clock](https://github.com/samuelbeckman/expressive-clock) by Sam Beckman, built on Material Clock by Rayane Bouafia, under the MIT licence.

## 1.7.1

Add transactions by talking, and delete several accounts at once.

### Voice notes in Add with AI

- **Tap the mic** next to the photo and file buttons and say what you spent, like "Lunch 240 at Chaayos yesterday on my HDFC card, and an Uber 180 this morning". Several payments in one go are fine.
- A **live waveform** moves as you talk. Tap the check to keep the recording or the X to throw it away. Recordings stop at 2 minutes.
- The recording goes to your AI model as audio, so it needs a model that listens, like Gemini. The mic only shows up when a model in your Add with AI ladder takes audio. Anthropic models don't, so voice notes skip them.
- The review page shows **what the model heard**, so a mishearing is easy to catch before you save.
- With receipts or files attached, what you said goes along with each file, the same as a typed note.
- A recording with no speech in it isn't sent. Given silence, models invent a whole note, amounts and all, so Paisa says it didn't catch anything instead.
- Paisa asks for the microphone the first time you tap the mic. Voice notes aren't in the web version yet.

### Accounts

- **Delete several accounts at once.** In the list view, long-press accounts to select them, then tap the trash button. It works with one account selected. Merge still needs two or more. The confirmation names every account, and their transactions are deleted with them.

## 1.7.0

Select many transactions at once, much better filters, and a redesign of Home's cards, Add with AI, SMS import and statement import. Plus fixes for day totals, app lock and AMOLED sheets.

### Select many at once

- **Long-press a transaction** to start selecting, then tap others. Long-press again to select every row in between, or tap a day's header to take the whole day.
- **Rename** them all: one name for all, find and replace (with an optional regex pattern), or text added before or after. A preview shows each change before you apply it.
- **Move** them to another category or account, or **delete** them. Renames and moves can be undone from the snackbar.

### Filters

- **Dates:** today, last 7 days, this month, last month, last 3 months, this year, or a custom range.
- **Amount:** a minimum, a maximum, or both.
- **Sources:** SMS, manual and statement, in any mix.
- Everything lives in one **Filters** sheet that shows how many transactions match as you change it. Applied filters show as chips you can tap to remove, and the list shows how much went out and came in.

### Home

- **Day totals count income.** A day with a ₹500 expense and ₹200 income now shows −₹300, not −₹500.
- **Credit cards and Debts** are compact cards in the same style as the balance card. Credit cards keep the usage dial and show what you owe of your limit. Debts shows the net and both totals without listing names. Tap it to open Debts.

### Add with AI

- A **chat-style composer**: your note, attachments and a Draft button in one box, with example prompts to start from.
- **While it reads**, Material shapes turn slowly under a large title, with your note, a seconds counter and a Stop button.

### Split with others

- **Search for people** instead of scrolling a strip of everyone. Type to find someone, or add a new name. Nobody is listed until you type.

### SMS import

- A status card with live scan progress and last-run counts, and permissions as rows you can tap to grant.
- **Dismiss all** clears the Not recognized list in one tap.
- **Mark inbox as scanned up to now** skips old messages without reading them. Scan new still rechecks the last 3 days.

### Backup and export

- **Delete a single backup** from Google Drive or Nextcloud.
- **Export data** can save a `.paisa` backup file as well as CSV and PDF. It's encrypted when backup encryption is on.

### Statement import

- Redesigned, with the AI model and supported files as lists. Choose files is always at the bottom and sets up AI if you haven't. Scanned PDFs work with models that read PDFs, like Gemini.

### Bank SMS

- New PhonePe wallet parser. Fixes for Slice, Priorbank, HDFC and PhonePe wallets, Islamic International Arab Bank, and ICICI OTP messages.
- Future-debit notices ("will be debited", UPI AutoPay mandates) are skipped, so the spend isn't counted twice.

### Fixes

- **App lock** opens the fingerprint or PIN prompt by itself when you come back to the app. Before, it only did on a cold start.
- **On AMOLED black**, bottom sheets now have a visible edge instead of blending into the screen.

## 1.6.4

Swipe between tabs, clearer chart tooltips, and a fix for the debts toolbar.

### Swipe to switch

- **Swipe between tabs.** On phones, swipe left or right anywhere on a tab to move to the next one in the bottom bar. Tapping a tab still jumps straight to it. Rows that scroll sideways, like the filter chips and account cards, keep their own swipe until they reach an end.
- **Swipe in the add sheet** to switch between Manual and With AI.

### Chart tooltips

- **They float next to what you tapped** instead of sitting in a strip above the chart, so the chart no longer jumps when a tooltip opens and there's no empty gap above it.
- **One row per series.** The running total shows this month and last month on their own lines, and says how much more or less you've spent. Income vs expense shows both and the net. A day this month hasn't reached says so.
- **Tall bars stay visible.** When there's no room above a point, the tooltip sits beside it.
- Colour dots have a thin ring, so pale colours show up on the tooltip.
- The spending donut shows the share of the tapped category in its centre, instead of repeating it in a tooltip.

### Fixes

- On a person's debt page, the toolbar sits just above the bottom bar instead of floating far above it.

## 1.6.3

Paisa now runs in the browser as well as on Android, from the same Kotlin code. This release also adds transactions with AI, a real first-run setup, and layouts that use the space on tablets and desktops.

### Paisa on the web

- **The whole app in a browser.** Home, Analytics, Budgets, Accounts, Debts, settings and backup all run in the browser. It's the same code as the phone, compiled to WebAssembly, not a separate web app.
- **Your data stays in the tab.** The database lives in the browser's private storage. Nothing is sent anywhere unless you turn on backup, sync or AI.
- **Sync with your phone.** A browser and a phone can share one Google Drive or Nextcloud and stay in step. The browser syncs when the page opens, every 15 minutes, when it comes back online, and 20 seconds after an edit.
- **Nextcloud from the browser.** Nextcloud doesn't allow browser requests, so there's a small proxy you can deploy to your own Cloudflare account. It forwards to your server and stores nothing. See `tools/nextcloud-proxy`.
- **Keyboard and mouse.** `n` adds, `/` or `Ctrl K` searches, `1` to `6` switch tabs, `Esc` closes things and `?` lists every shortcut. Right-click a transaction to open or delete it. Charts show their tooltip on hover.
- **Demo data.** Add `?demo` to the address to try it with three months of sample data.
- **Not on the web yet:** reading SMS, PDF export, reading PDF statements, and app lock.

### Add transactions with AI

- The add sheet has a **Manual / With AI** switch. Describe payments in your own words, like "lunch 240 yesterday, uber 180 today", attach receipts, bills, payment screenshots or PDFs, or do both.
- You get a list of drafts to check before anything is saved. Tap one to change its amount, title, date, time, category or account, or untick the ones you don't want.
- **Ask for a change.** Type "the uber one was cash" under the drafts and the AI updates them.
- Several files are read at once. Your note goes along with each file, so it can fill in what a receipt doesn't show, like which card paid.
- Drafts that look like something already logged, say from a bank SMS, start unticked with a note saying what they matched.
- It has its own **Add with AI** ladder in More → AI, so you can give it a model that reads images.

### First-run setup

- The ten-page tour is replaced by a short setup: what Paisa does with your data, your currency, your theme and palette with a live preview, and on Android, SMS access with an example of an alert turning into a transaction.
- It ends with shortcuts to add a transaction, set up AI, back up or sync, or import a statement.
- **I have a backup** on the first page restores one instead.
- About → **Run setup again** brings it back.

### Tablets, foldables and desktops

- **The layout follows the window.** Phones keep the bottom bar. From 600dp there's a side rail, and from 1200dp a drawer listing every tab.
- **Side by side.** On wide screens a transaction opens beside the list, Debts and settings show a list with the open item next to it, Analytics uses two columns, and Budgets and Accounts use grids.
- **Dialogs instead of sheets** on wide screens, sized to the content.
- On Android tablets, the add and rescan buttons stay floating in the corner. In a browser they sit at the top of the rail or drawer.

### Look and feel

- **Pill tabs** replace the segmented buttons for the Analytics period, spending or income, debt direction, and split mode.
- **Floating toolbars** on a person's debt page (They owe me, I owe them, Settle up) and when picking accounts to merge.
- **Info tips** explain credit usage, the running total, the spending heatmap, income vs expense, budgets and sync.
- **Grouped transactions.** Each day's transactions sit together in one block, like the sections in More. Account transaction lists gain day headers.
- **Icon colors** on About and the AI services list vary row by row, like More.
- **About** says where your data goes, shows the version and platform, and links to the source code, issues, releases, license and the PennyWise SMS parsers.

### Fixes

- Saving an edit no longer jumps the transaction list back to the top. Home, Analytics and Accounts keep their place when a screen covers them or you switch tabs.
- On phones the snackbar sits above the bottom bar, and the buttons slide out of its way instead of the message covering the list.
- The theme picker's highlight no longer spills outside its rounded cards.
- On the web, a request the browser blocks now fails within seconds with a reason, instead of a spinner that ran for three minutes.
- A phone browser with "Desktop site" turned on gets the phone layout instead of a cut-off desktop one.

### Under the hood

- The app is now Kotlin Multiplatform. Screens, view models, the database, sync and the AI layer are shared between Android and the web.
- Koin replaces Hilt, Room 3 replaces Room 2, and the AI client uses Ktor.
- The database format is unchanged, so updating keeps your data, settings and AI keys, and older backups still restore.

## 1.5.2

- **Tablets and foldables.** Windows 600dp and wider get a navigation rail instead of the bottom bar. At 840dp and wider, a transaction opens in a pane beside the list, split down the middle so it lands on a book-style fold's hinge. Lists, settings, onboarding and the lock screen stop at 840dp and center instead of stretching.
- Folding, unfolding or rotating no longer closes open sheets and screens or clears what you typed.

## 1.5.1

A small update to Analytics: the Top places and By account lists are easier to read.

- **Places show what they are.** Each place uses the icon and colour of the category most of its money went to, and names it underneath, like "Groceries · 3 payments".
- **Accounts show their type.** Bank accounts, cash and cards each get their own icon, with the number of payments underneath.
- **Long names fit.** The amount and its share sit in their own column, so long names no longer run into the amount. Whole amounts drop the ".00".
- **Cleaner bars.** Each row's bar is slimmer, full width, and in that row's colour.

## 1.5.0

Paisa 1.5 keeps debts in step with your transactions, adds splitting, and rebuilds the AI setup so you can choose providers, models and fallbacks for each job.

### Debts and splitting

- **Debts follow their transactions.** Change a split transaction's amount and each share changes with it. Delete the transaction and its debts go too.
- **Split with others** when you add an expense, equally or by custom amounts, with a preview of each share before you save.
- **Edit or remove a split** later from the transaction's page.
- **Split a bill** from the Debts tab, whoever paid.
- **Someone paid me back** marks money you received as a repayment.
- Amounts you added by hand can be edited or deleted, and splits edited on two devices sync without doubling up.

### AI

- **More providers.** OpenAI, Anthropic, Google Gemini and your own RelayRoom gateway are built in, with OpenRouter, DeepSeek, Groq, NVIDIA and OpenCode Zen as presets, plus any custom server in the OpenAI, Anthropic or Gemini format.
- **A model catalogue** from models.dev and each provider, with badges for image, PDF and thinking support.
- **Ladders for each job.** Models are tried top to bottom, and if one fails the next one runs.
- **Thinking levels**, scanned PDFs sent to models that read them, and real provider logos.
- Your existing provider and key move over automatically.

### Security

- Plain http only goes to this device, an emulator host or a private network. Requests that carry a key don't follow redirects, and keys stay out of backups.

## 1.4.0

Paisa 1.4 rebuilds Analytics around any time range you pick.

- **Month, Year or Custom**, down to a start and end time, with one-tap ranges for today and the last 7, 30 and 90 days.
- **More charts:** a running total against the period before, spending by day of the week, top places, and spending by account.
- **Summary cards** show the change from the period before, and the quick stats show your daily average, count, biggest expense and the difference.
- **Drill-down pages** for a category, place or account list every transaction in it.
- Account cards use your Material You colours.
- Messages sent from a full-screen page now show when you get back.
- Under the hood, every screen got its own package and ViewModel, and the app moved to Hilt.

## 1.3.0

- **Debts.** A new section for money people owe you and money you owe them, with a page per person, Settle up, and a Debts card on Home. Debts never change your balances, Analytics or Budgets, and they sync and back up.
- **Someone owes me for this** on a new or existing transaction counts all or part of it as a debt.
- **Pick your navbar tabs** in More → Appearance. Tabs you turn off open from More → Screens.
- Backups made on 1.3 need 1.3 or newer to restore.

## 1.2.0

- **Sync between devices** over Google Drive or Nextcloud, encrypted with your sync password. Edits sync about 20 seconds later, on open, and every hour. The same bank SMS on two phones counts once, and the later edit wins.
- **Floating navbar** and **navbar labels** options in More → Appearance.
- **Balances** are the last known balance plus every transaction after it, so two devices show the same number. Updating keeps every balance as it was.

## 1.1.0

- **Backup and restore** to Google Drive, Nextcloud or a `.paisa` file, automatically or on demand, optionally encrypted with AES-256-GCM. Paisa keeps your newest three cloud backups.
- **Restore from onboarding** on a new install.
- Android's own Auto Backup no longer copies your AI keys or backup credentials.

## 1.0.0

The first release.

- **Home** with your balance, income, spending, filters and recent transactions.
- **Analytics** by category over the date range you pick.
- **Budgets** with a monthly cap per category.
- **Accounts** for cash, banks, cards and wallets, which you can reorder, merge, set a default on and correct by hand.
- **SMS import** from about 140 Indian banks, once in bulk and then live as new SMS arrive, without importing anything twice.
- **Statement import** with your own AI key, for PDF, CSV and image statements.
- **App lock**, **CSV and PDF export**, and **home screen widgets**.
