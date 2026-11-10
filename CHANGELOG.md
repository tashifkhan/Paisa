# Changelog

Every Paisa release, newest first. Each version's APK is on the [releases page](https://github.com/tashifkhan/Paisa/releases) and in the F-Droid repo at `https://tashif.codes/fdroid/repo`.

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
