package com.shohankhan.hisaabos.ui.text

import com.shohankhan.hisaabos.domain.model.AccountType
import com.shohankhan.hisaabos.domain.model.BudgetPeriod
import com.shohankhan.hisaabos.domain.model.CategoryKind
import com.shohankhan.hisaabos.domain.model.DebtDirection
import com.shohankhan.hisaabos.domain.model.InterestModel
import com.shohankhan.hisaabos.domain.model.LoanKind
import com.shohankhan.hisaabos.domain.model.RecurrenceFrequency
import com.shohankhan.hisaabos.domain.model.RepaymentFrequency
import com.shohankhan.hisaabos.domain.model.SavingsKind
import com.shohankhan.hisaabos.domain.model.TxType

/**
 * Central catalogue of user-facing Bangla copy.
 *
 * HisaabOS is Bangla-first; keeping every string in one reviewed object makes it
 * impossible for a stray English label to slip into the UI and keeps wording
 * consistent across screens. App name / owner details live in resources.
 */
object Bn {
    const val appName = "হিসাবওএস"
    const val brandTagline = "আপনার সব টাকার হিসাব, এক জায়গায়"
    const val offlineNote = "সম্পূর্ণ অফলাইন · কোনো ইন্টারনেট বা অ্যাকাউন্ট লাগবে না · আপনার তথ্য আপনার ফোনেই থাকবে"

    // Navigation
    const val navHome = "হোম"
    const val navTransactions = "লেনদেন"
    const val navBooks = "হিসাব"
    const val navReports = "রিপোর্ট"
    const val navMore = "আরও"

    // Onboarding
    const val welcomeTitle = "স্বাগতম"
    const val start = "শুরু করুন"
    const val setUpPin = "পিন তৈরি করুন"
    const val pinExplainer = "৪–৮ সংখ্যার একটি পিন দিন। এই পিন দিয়েই অ্যাপ খুলতে হবে, তাই অন্য কেউ আপনার হিসাব দেখতে পারবে না।"
    const val pinWarning = "পিন ভুলে গেলে ব্যাকআপ ছাড়া তথ্য উদ্ধার করা যাবে না। গুরুত্বপূর্ণ হিসাব নিয়মিত ব্যাকআপ করুন।"
    const val openingBalanceTitle = "শুরুর ব্যালেন্স"
    const val openingBalanceExplainer = "আপনার কাছে এখন যে টাকা আছে তা লিখুন। এটি আয় নয়, শুধু শুরুর অবস্থা।"
    const val skipForNow = "এখন বাদ দিন"
    const val continueLabel = "চালিয়ে যান"
    const val finish = "শেষ করুন"
    const val tourTitle = "যা যা করতে পারবেন"
    const val gotIt = "বুঝেছি"

    // Lock
    const val enterPin = "পিন দিন"
    const val pinIncorrect = "পিন মিলছে না"
    const val unlock = "খুলুন"
    const val useBiometric = "ফিঙ্গারপ্রিন্ট ব্যবহার করুন"
    const val forgotPin = "পিন ভুলে গেছেন?"
    const val forgotPinTitle = "পিন পুনরুদ্ধার"
    const val forgotPinBody = "নিরাপত্তার কারণে পিন উদ্ধার করার কোনো গোপন উপায় নেই। ব্যাকআপ থাকলে তা থেকে তথ্য ফিরিয়ে আনা যায়। ব্যাকআপ না থাকলে নতুন করে শুরু করতে হলে সব তথ্য মুছে যাবে।"
    const val restoreFromBackup = "ব্যাকআপ থেকে ফিরিয়ে আনুন"
    const val resetAndStartOver = "সব মুছে নতুন করে শুরু করুন"
    const val typeToConfirm = "নিশ্চিত করতে নিচে “মুছুন” লিখুন"
    const val confirmWord = "মুছুন"
    const val cancel = "বাতিল"
    const val delete = "মুছুন"
    const val save = "সংরক্ষণ"
    const val confirm = "নিশ্চিত করুন"
    const val close = "বন্ধ"

    // Common
    const val add = "যোগ করুন"
    const val edit = "সম্পাদনা"
    const val back = "ফিরে যান"
    const val search = "খুঁজুন"
    const val all = "সব"
    const val none = "কিছু নেই"
    const val today = "আজ"
    const val total = "মোট"
    const val remaining = "বাকি"
    const val paid = "পরিশোধিত"
    const val date = "তারিখ"
    const val amount = "টাকার পরিমাণ"
    const val note = "বিস্তারিত লিখুন"
    const val noteOptional = "বিস্তারিত (ঐচ্ছিক)"
    const val account = "হিসাব"
    const val category = "ক্যাটাগরি"
    const val shop = "দোকান"
    const val person = "ব্যক্তি"
    const val dueDate = "ফেরতের তারিখ"
    const val optional = "ঐচ্ছিক"
    const val loading = "একটু অপেক্ষা করুন…"
    const val retry = "আবার চেষ্টা করুন"
    const val saved = "সংরক্ষণ হয়েছে"
    const val deleted = "মুছে ফেলা হয়েছে"
    const val noData = "কোনো তথ্য নেই"
    const val learnMore = "বিস্তারিত"
    const val viewAll = "সব দেখুন"
    const val create = "তৈরি করুন"
    const val update = "হালনাগাদ"
    const val required = "আবশ্যক"
    const val yes = "হ্যাঁ"
    const val no = "না"

    // Dashboard
    const val greeting = "আসসালামু আলাইকুম"
    const val totalAssets = "মোট সম্পদ"
    const val totalLiabilities = "মোট দায়"
    const val netWorth = "নিট সম্পদ"
    const val availableCash = "উপলভ্য নগদ ও ব্যাংক"
    const val todayIncome = "আজকের আয়"
    const val todayExpense = "আজকের ব্যয়"
    const val todayCashFlow = "আজকের নিট নগদ প্রবাহ"
    const val quickActions = "দ্রুত কাজ"
    const val moreActions = "আরও কাজ"
    const val upcoming = "আসন্ন দায়"
    const val noUpcoming = "এখন কোনো আসন্ন দায় নেই"
    const val recentActivity = "সাম্প্রতিক লেনদেন"
    const val noTransactions = "এখনো কোনো লেনদেন নেই"
    const val noTransactionsHint = "নিচের + বাটন চেপে প্রথম লেনদেন যোগ করুন"
    const val insights = "আপনার হিসাবের হাল"
    const val hideAmounts = "টাকার হিসাব লুকান"
    const val showAmounts = "টাকার হিসাব দেখান"
    const val emptyAccountHint = "শুরু করতে একটি হিসাব তৈরি করুন"

    // Actions
    const val addIncome = "আয় যোগ"
    const val addExpense = "খরচ যোগ"
    const val addMarket = "বাজার যোগ"
    const val creditPurchase = "বাকিতে কেনা"
    const val borrowMoney = "টাকা ধার নেওয়া"
    const val lendMoney = "টাকা ধার দেওয়া"
    const val payMoney = "টাকা পরিশোধ"
    const val savingsDeposit = "সঞ্চয় জমা"
    const val transfer = "হিসাব স্থানান্তর"
    const val adjust = "সমন্বয়"
    const val addTransaction = "লেনদেন যোগ"

    // Transactions
    const val howMuch = "কত টাকা?"
    const val forWhat = "কিসের জন্য?"
    const val fromWhichAccount = "কোন হিসাব থেকে?"
    const val toWhichAccount = "কোন হিসাবে?"
    const val whenDate = "কবে?"
    const val saving = "সংরক্ষণ করা হচ্ছে…"
    const val duplicateWarning = "একই দিনে একই অঙ্কের লেনদেন আগেও আছে। নিশ্চিত হলে সংরক্ষণ করুন।"
    const val filter = "ফিল্টার"
    const val sort = "সাজান"
    const val newest = "নতুন আগে"
    const val oldest = "পুরনো আগে"
    const val highest = "বড় অঙ্ক আগে"
    const val lowest = "ছোট অঙ্ক আগে"
    const val dateRange = "তারিখের সীমা"
    const val typeFilter = "লেনদেনের ধরন"
    const val minAmount = "সর্বনিম্ন অঙ্ক"
    const val maxAmount = "সর্বোচ্চ অঙ্ক"
    const val clearFilters = "ফিল্টার মুছুন"
    const val apply = "প্রয়োগ করুন"
    const val results = "ফলাফল"
    const val noResults = "এই শর্তে কোনো লেনদেন নেই"
    const val deleteTransaction = "লেনদেন মুছবেন?"
    const val deleteTransactionBody = "মুছে ফেললে এটি সব রিপোর্ট ও ব্যালেন্স থেকে বাদ যাবে। এটি অডিট লগে সংরক্ষিত থাকবে।"
    const val transactionDetail = "লেনদেনের বিবরণ"
    const val typeLabel = "ধরন"
    const val attachment = "রসিদ"
    const val addReceipt = "রসিদের ছবি যোগ করুন"

    // Accounts / categories
    const val accounts = "হিসাব ও ওয়ালেট"
    const val accountName = "হিসাবের নাম"
    const val accountType = "হিসাবের ধরন"
    const val institution = "প্রতিষ্ঠান"
    const val openingBalance = "প্রারম্ভিক ব্যালেন্স"
    const val currentBalance = "বর্তমান ব্যালেন্স"
    const val archived = "সংরক্ষিত"
    const val archive = "আর্কাইভ করুন"
    const val unarchive = "ফিরিয়ে আনুন"
    const val newAccount = "নতুন হিসাব"
    const val reconcile = "হিসাব মেলান"
    const val reconcileHint = "আসল ব্যালেন্স লিখুন। পার্থক্য থাকলে শুধু সমন্বয় লেনদেন তৈরি হবে, কোনো আয় বা ব্যয় নয়।"
    const val statementBalance = "আসল ব্যালেন্স"
    const val adjustmentReason = "কারণ"
    const val categories = "ক্যাটাগরি"
    const val newCategory = "নতুন ক্যাটাগরি"
    const val categoryName = "ক্যাটাগরির নাম"
    const val categoryKind = "ধরন"
    const val incomeCategory = "আয়ের ক্যাটাগরি"
    const val expenseCategory = "খরচের ক্যাটাগরি"
    const val categoryArchived = "আর্কাইভ করা ক্যাটাগরি পুরনো লেনদেনে থাকবে"
    const val deleteCategory = "ক্যাটাগরি মুছবেন?"
    const val deleteCategoryBody = "এই ক্যাটাগরির লেনদেন থাকলে সেগুলো অন্য ক্যাটাগরিতে সরাতে হবে, নাহলে ক্যাটাগরি শুধু আর্কাইভ করা যাবে।"
    const val reassign = "অন্য ক্যাটাগরিতে সরান"

    // Shops / people
    const val shops = "দোকান ও দোকানদার"
    const val people = "ব্যক্তি ও পরিচিতজন"
    const val shopName = "দোকানের নাম"
    const val shopkeeper = "দোকানদারের নাম"
    const val phone = "মোবাইল নম্বর"
    const val address = "ঠিকানা"
    const val payable = "বাকি (দিতে হবে)"
    const val receivable = "পাওনা (নিতে হবে)"
    const val shopLedger = "দোকানের হিসাব"
    const val personLedger = "ব্যক্তির হিসাব"
    const val creditPurchases = "বাকিতে কেনা"
    const val payment = "পরিশোধ"
    const val newShop = "নতুন দোকান"
    const val newPerson = "নতুন ব্যক্তি"
    const val items = "পণ্য"
    const val purchaseHistory = "বাজারের ইতিহাস"
    const val noShops = "কোনো দোকান যোগ করা হয়নি"
    const val noPeople = "কোনো ব্যক্তি যোগ করা হয়নি"
    const val shopHint = "বাকিতে বাজার করলে দোকানভিত্তিক হিসাব রাখা যায়"

    // Purchases
    const val marketTitle = "বাজার / ক্রয় যোগ"
    const val productName = "পণ্যের নাম"
    const val quantity = "পরিমাণ"
    const val unit = "একক"
    const val unitPrice = "এককের দাম"
    const val lineTotal = "মোট দাম"
    const val addItem = "পণ্য যোগ করুন"
    const val discount = "ছাড়"
    const val extraCharge = "অতিরিক্ত চার্জ"
    const val paidNow = "এখন পরিশোধ"
    const val onCredit = "বাকি"
    const val grandTotal = "মোট"
    const val purchaseSaved = "বাজার সংরক্ষণ হয়েছে"
    const val noItems = "কোনো পণ্য নেই"

    // Debts / loans / EMI
    const val debtBorrowed = "ধার নেওয়া"
    const val debtLent = "ধার দেওয়া"
    const val borrowed = "নেওয়া ধার"
    const val lent = "দেওয়া ধার"
    const val principal = "মূল টাকা"
    const val interest = "সুদ"
    const val fee = "ফি / চার্জ"
    const val lenderName = "ঋণদাতার নাম"
    const val borrowerName = "ঋণগ্রহীতার নাম"
    const val returnDate = "ফেরতের তারিখ"
    const val partialPayment = "আংশিক পরিশোধ"
    const val collect = "আদায় করুন"
    const val repay = "পরিশোধ করুন"
    const val loansTitle = "ঋণ ও কিস্তি"
    const val ngoLoan = "এনজিও ঋণ"
    const val bankLoan = "ব্যাংক ঋণ"
    const val emiPurchase = "কিস্তিতে কেনা"
    const val newLoan = "নতুন ঋণ"
    const val newEmi = "নতুন কিস্তি চুক্তি"
    const val loanTerms = "ঋণের শর্ত"
    const val interestModel = "সুদের ধরন"
    const val flatTotal = "নির্দিষ্ট মোট কিস্তি (ফ্ল্যাট)"
    const val reducing = "কমতিরোধী (রেডিউসিং)"
    const val zeroInterest = "সুদ নেই"
    const val manualSchedule = "নিজে কিস্তি লিখব"
    const val annualRate = "বার্ষিক সুদের হার (%)"
    const val frequency = "কিস্তির ধরন"
    const val weekly = "সাপ্তাহিক"
    const val biweekly = "দ্বি-সাপ্তাহিক"
    const val monthly = "মাসিক"
    const val once = "এককালীন"
    const val installmentCount = "কিস্তির সংখ্যা"
    const val installmentAmount = "কিস্তির অঙ্ক"
    const val firstDue = "প্রথম কিস্তির তারিখ"
    const val totalRepayable = "মোট পরিশোধযোগ্য"
    const val remainingPrincipal = "বাকি মূল টাকা"
    const val remainingTotal = "বাকি মোট"
    const val schedule = "কিস্তির সময়সূচি"
    const val estimatedSchedule = "এটি অনুমানভিত্তিক সময়সূচি; প্রতিষ্ঠানের অফিসিয়াল হিসাবের সাথে মিলিয়ে নিন"
    const val markPaid = "পরিশোধ করুন"
    const val loanClosed = "ঋণ বন্ধ হয়েছে"
    const val noLoans = "কোনো ঋণ যোগ করা হয়নি"
    const val productTitle = "পণ্যের নাম"
    const val merchant = "দোকান / বিক্রেতা"
    const val cashPrice = "নগদ মূল্য"
    const val financedPrice = "মোট অর্থায়িত মূল্য"
    const val downPayment = "ডাউন পেমেন্ট"
    const val financedAmount = "অর্থায়নের পরিমাণ"
    const val totalInstallment = "মোট কিস্তি"
    const val interestFree = "সুদমুক্ত"
    const val payInstallment = "কিস্তি দিন"

    // Savings
    const val savings = "সঞ্চয়"
    const val savingsTarget = "লক্ষ্যমাত্রা"
    const val targetDate = "লক্ষ্যের তারিখ"
    const val contribution = "নিয়মিত জমা"
    const val deposit = "জমা দিন"
    const val withdraw = "উত্তোলন"
    const val savingsHistory = "জমা ও উত্তোলন"
    const val newSavings = "নতুন সঞ্চয়"
    const val noSavings = "কোনো সঞ্চয় পরিকল্পনা নেই"
    const val savingsHint = "নিজের হিসাবে সঞ্চয় সরানো খরচ নয় — এটি শুধু টাকা সরানো"
    const val goalProgress = "লক্ষ্যের অগ্রগতি"

    // Budgets
    const val budgets = "বাজেট"
    const val newBudget = "নতুন বাজেট"
    const val budgetLimit = "সীমা"
    const val spent = "খরচ হয়েছে"
    const val budgetRemaining = "বাকি আছে"
    const val overBudget = "সীমা ছাড়িয়েছে"
    const val noBudgets = "কোনো বাজেট নেই"
    const val budgetHint = "নিজের তৈরি ক্যাটাগরি বেছে নিয়ে মাসিক বা সাপ্তাহিক খরচের সীমা দিন"
    const val allCategories = "সব ক্যাটাগরি"

    // Reports
    const val reportsTitle = "রিপোর্ট"
    const val summary = "সারসংক্ষেপ"
    const val incomeVsExpense = "আয় ও ব্যয়"
    const val categoryBreakdown = "ক্যাটাগরি অনুযায়ী খরচ"
    const val accountBalances = "হিসাবের ব্যালেন্স"
    const val cashFlow = "নগদ প্রবাহ"
    const val shopReport = "দোকানভিত্তিক বাকি"
    const val debtReport = "ধার ও পাওনা"
    const val loanReport = "ঋণের সারসংক্ষেপ"
    const val savingsReport = "সঞ্চয়ের সারসংক্ষেপ"
    const val budgetReport = "বাজেটের অবস্থা"
    const val exportCsv = "CSV ডাউনলোড"
    const val noReportData = "এই সময়ে কোনো তথ্য নেই"
    const val noReportHint = "লেনদেন যোগ করলে রিপোর্ট এখানে দেখা যাবে"
    const val monthlyTrend = "মাসিক ধারা"
    const val periodToday = "আজ"
    const val periodWeek = "এই সপ্তাহ"
    const val periodMonth = "এই মাস"
    const val periodYear = "এই বছর"
    const val periodAll = "সব সময়"
    const val customRange = "নিজের তারিখ"

    // More / settings
    const val moreTitle = "আরও"
    const val financesGroup = "আপনার হিসাব"
    const val planningGroup = "পরিকল্পনা"
    const val dataGroup = "তথ্য ও নিরাপত্তা"
    const val otherGroup = "অন্যান্য"
    const val recurring = "নিয়মিত লেনদেন"
    const val reminders = "মনে করানো"
    const val backup = "ব্যাকআপ ও পুনরুদ্ধার"
    const val exportData = "তথ্য রপ্তানি"
    const val security = "নিরাপত্তা ও পিন"
    const val appearance = "দেখতে কেমন"
    const val language = "ভাষা"
    const val about = "হিসাবওএস সম্পর্কে"
    const val verifyBooks = "হিসাব যাচাই"
    const val verifyBooksBody = "প্রতিটি লেনদেনের সাথে ব্যালেন্সের পোস্টিং মিলিয়ে দেখা হবে।"
    const val booksOk = "সব হিসাব মিলে গেছে"
    const val booksIssues = "কিছু অমিল পাওয়া গেছে"

    // Backup
    const val backupTitle = "ব্যাকআপ ও পুনরুদ্ধার"
    const val createBackup = "ব্যাকআপ ফাইল তৈরি করুন"
    const val restoreBackup = "ব্যাকআপ থেকে ফিরিয়ে আনুন"
    const val backupEncrypt = "পাসওয়ার্ড দিয়ে সুরক্ষিত করুন"
    const val backupPassphrase = "পাসওয়ার্ড"
    const val backupPassphraseHint = "এই পাসওয়ার্ড ছাড়া ব্যাকআপ খোলা যাবে না। ভুলে গেলে কেউ উদ্ধার করতে পারবে না।"
    const val backupPlainWarning = "সুরক্ষিত না করলে ফাইলটি যে কেউ পড়তে পারবে।"
    const val backupWarning = "ব্যাকআপে পিন থাকে না। ফিরিয়ে আনার পর নতুন পিন দিতে হবে।"
    const val lastBackup = "সর্বশেষ ব্যাকআপ"
    const val never = "কখনো নয়"
    const val restoreModeTitle = "কীভাবে ফিরিয়ে আনবেন?"
    const val restoreReplace = "সব প্রতিস্থাপন করুন"
    const val restoreReplaceBody = "বর্তমান সব তথ্য মুছে ব্যাকআপের তথ্য বসবে।"
    const val restoreMerge = "বর্তমানের সাথে যোগ করুন"
    const val restoreMergeBody = "বর্তমান হিসাব থাকবে, ব্যাকআপের নতুন তথ্য যোগ হবে।"
    const val restoreDone = "ফিরিয়ে আনা হয়েছে"
    const val backupDone = "ব্যাকআপ তৈরি হয়েছে"
    const val csvExport = "CSV রপ্তানি"
    const val chooseFile = "ফাইল বাছুন"
    const val invalidBackup = "ব্যাকআপ ফাইলটি বৈধ নয়"
    const val passphraseRequired = "পাসওয়ার্ড প্রয়োজন"

    // Security
    const val changePin = "পিন পরিবর্তন"
    const val newPin = "নতুন পিন"
    const val confirmPin = "পিনটি আবার দিন"
    const val currentPin = "বর্তমান পিন"
    const val pinMismatch = "দুটি পিন মিলছে না"
    const val biometric = "ফিঙ্গারপ্রিন্ট / বায়োমেট্রিক"
    const val biometricHint = "ডিভাইসে বায়োমেট্রিক থাকলে পিনের পাশাপাশি ব্যবহার করা যাবে"
    const val biometricUnavailable = "এই ডিভাইসে বায়োমেট্রিক সমর্থিত নয়"
    const val autoLock = "স্বয়ংক্রিয় লক"
    const val immediately = "সাথে সাথেই"
    const val after30s = "৩০ সেকেন্ড পরে"
    const val after1m = "১ মিনিট পরে"
    const val after5m = "৫ মিনিট পরে"
    const val autoLockHint = "অ্যাপ থেকে বেরোনোর পর এই সময় পার হলে আবার পিন লাগবে"
    const val screenProtection = "স্ক্রিনশট ও রিসেন্ট থেকে সুরক্ষা"
    const val lockNow = "এখনই লক করুন"
    const val pinChangeWarning = "পিন পরিবর্তন করলে পুরনো ব্যাকআপের সাথে কোনো সম্পর্ক নেই; ব্যাকআপে পিন থাকে না।"
    const val wrongCurrentPin = "বর্তমান পিন মিলছে না"

    // Reports/insights
    const val insightTopCategory = "সবচেয়ে বেশি খরচ"
    const val insightVsPrevious = "আগের সময়ের তুলনায়"
    const val insightSavings = "সঞ্চয়ের অগ্রগতি"
    const val insightDebt = "মোট দায়"
    const val insightInstallment = "আসন্ন কিস্তির চাপ"
    const val noInsight = "খুব কম তথ্য — আরও লেনদেন যোগ করুন"

    // App-level
    const val offlineBanner = "আপনি অফলাইনে আছেন — তবুও সব কাজ চলবে"
    const val appDescription = "হিসাবওএস একটি সম্পূর্ণ অফলাইন ব্যক্তিগত ও পারিবারিক হিসাবরক্ষণ অ্যাপ। আয়, ব্যয়, বাজার, দেনা-পাওনা, ঋণ-কিস্তি, সঞ্চয় ও বাজেট — সব এক জায়গায়, নিজের ফোনেই।"
    const val contactOwner = "যোগাযোগ করুন"
    const val version = "সংস্করণ"
    const val madeWithCare = "বাংলাদেশের ঘরোয়া হিসাবের জন্য তৈরি"
    const val privacyTitle = "গোপনীয়তা"
    const val privacyBody = "আপনার কোনো তথ্য কোথাও পাঠানো হয় না। অ্যাপে ইন্টারনেট অনুমতিই নেই। সব হিসাব ফোনের নিজস্ব ডেটাবেসে থাকে।"
    const val licensesTitle = "ওপেন সোর্স কৃতজ্ঞতা"
    const val licensesBody = "হিন্দ শিলিগুড়ি ফন্ট (SIL Open Font License) · Jetpack Compose, Room, Kotlin ও অন্যান্য AndroidX লাইব্রেরি (Apache 2.0)।"
    const val resetAllData = "সব তথ্য মুছে ফেলুন"
    const val resetWarning = "এই কাজটি ফিরিয়ে আনা যাবে না। আগে ব্যাকআপ নিন।"

    // Recurring / reminders
    const val newReminder = "নতুন মনে করানো"
    const val reminderTitle = "কী মনে করাবেন?"
    const val reminderDate = "কবে?"
    const val markDone = "সম্পন্ন"
    const val recurringHint = "নিয়মিত লেনদেন শুধু মনে করায়; নিজে থেকে টাকা কাটে না"
    const val noReminders = "কোনো মনে করানো নেই"
    const val notifyHint = "সময়মতো মনে করানোর জন্য নোটিফিকেশন অনুমতি দিতে হবে"

    // Search
    const val searchTitle = "সার্চ"
    const val searchHint = "নাম, দোকান, ক্যাটাগরি বা নোট লিখুন"
    const val searchTransactions = "লেনদেন"
    const val searchShops = "দোকান"
    const val searchPeople = "ব্যক্তি"
    const val searchCategories = "ক্যাটাগরি"
    const val noSearchResults = "কিছু পাওয়া যায়নি"

    // Errors
    const val errorGeneric = "কিছু একটা ভুল হয়েছে। আবার চেষ্টা করুন।"
    const val errorSave = "সংরক্ষণ করা যায়নি। আপনার লেখা তথ্য মুছে যায়নি — আবার চেষ্টা করুন।"
    const val errorAccountRequired = "আগে একটি হিসাব তৈরি করুন"
    const val errorAmount = "সঠিক টাকার পরিমাণ লিখুন"

    fun txType(type: TxType): String = when (type) {
        TxType.INCOME -> "আয়"
        TxType.EXPENSE -> "খরচ"
        TxType.TRANSFER -> "হিসাব স্থানান্তর"
        TxType.CREDIT_PURCHASE -> "বাকিতে কেনা"
        TxType.SHOP_PAYMENT -> "দোকানের বিল পরিশোধ"
        TxType.DEBT_BORROWED -> "টাকা ধার নেওয়া"
        TxType.DEBT_REPAID -> "ধার পরিশোধ"
        TxType.LENDING -> "টাকা ধার দেওয়া"
        TxType.LOAN_COLLECTION -> "পাওনা আদায়"
        TxType.LOAN_DISBURSEMENT -> "ঋণ গ্রহণ"
        TxType.LOAN_REPAYMENT -> "ঋণ পরিশোধ"
        TxType.EMI_PURCHASE -> "কিস্তিতে ক্রয়"
        TxType.EMI_PAYMENT -> "কিস্তি পরিশোধ"
        TxType.SAVINGS_DEPOSIT -> "সঞ্চয় জমা"
        TxType.SAVINGS_WITHDRAWAL -> "সঞ্চয় উত্তোলন"
        TxType.OPENING_BALANCE -> "শুরুর ব্যালেন্স"
        TxType.ADJUSTMENT -> "সমন্বয়"
        TxType.REFUND -> "ফেরত"
    }

    fun accountType(type: AccountType): String = when (type) {
        AccountType.CASH -> "নগদ"
        AccountType.BANK -> "ব্যাংক"
        AccountType.MFS -> "মোবাইল ব্যাংকিং"
        AccountType.WALLET -> "ওয়ালেট"
        AccountType.SAVINGS -> "সঞ্চয়"
        AccountType.CREDIT_CARD -> "ক্রেডিট কার্ড"
        AccountType.OTHER -> "অন্যান্য"
    }

    fun savingsKind(kind: SavingsKind): String = when (kind) {
        SavingsKind.CASH -> "নগদ সঞ্চয়"
        SavingsKind.BANK -> "ব্যাংক জমা"
        SavingsKind.NGO -> "এনজিও সঞ্চয়"
        SavingsKind.RECURRING -> "নিয়মিত সঞ্চয়"
        SavingsKind.GOAL -> "ব্যক্তিগত লক্ষ্য"
        SavingsKind.OTHER -> "অন্যান্য"
    }

    fun loanKind(kind: LoanKind): String = if (kind == LoanKind.NGO) "এনজিও ঋণ" else "ব্যাংক ঋণ"

    fun direction(direction: DebtDirection): String =
        if (direction == DebtDirection.BORROWED) "ধার নেওয়া" else "ধার দেওয়া"

    fun interestModel(model: InterestModel): String = when (model) {
        InterestModel.FLAT_TOTAL -> flatTotal
        InterestModel.REDUCING_BALANCE -> reducing
        InterestModel.ZERO_INTEREST -> zeroInterest
        InterestModel.MANUAL -> manualSchedule
    }

    fun frequency(value: RepaymentFrequency): String = when (value) {
        RepaymentFrequency.WEEKLY -> weekly
        RepaymentFrequency.BIWEEKLY -> biweekly
        RepaymentFrequency.MONTHLY -> monthly
        RepaymentFrequency.ONCE -> once
    }

    fun recurrence(value: RecurrenceFrequency): String = when (value) {
        RecurrenceFrequency.DAILY -> "প্রতিদিন"
        RecurrenceFrequency.WEEKLY -> "প্রতি সপ্তাহে"
        RecurrenceFrequency.MONTHLY -> "প্রতি মাসে"
    }

    fun budgetPeriod(value: BudgetPeriod): String = when (value) {
        BudgetPeriod.WEEKLY -> "সাপ্তাহিক"
        BudgetPeriod.MONTHLY -> "মাসিক"
        BudgetPeriod.CUSTOM -> "নিজের সময়"
    }

    fun categoryKind(value: CategoryKind): String = when (value) {
        CategoryKind.INCOME -> "আয়"
        CategoryKind.EXPENSE -> "খরচ"
        CategoryKind.BOTH -> "উভয়"
    }

    // Onboarding tour items
    val tourItems: List<Pair<String, String>> = listOf(
        "হিসাব যোগ" to "আয়, খরচ, বাজার বা বাকি — যেকোনো লেনদেন কয়েক সেকেন্ডে যোগ করুন।",
        "দেনা-পাওনা" to "কাকে কত টাকা দিতে হবে, কার কাছে পাওনা — সব আলাদা করে রাখুন।",
        "ঋণ ও কিস্তি" to "এনজিও, ব্যাংক ঋণ ও কিস্তির হিসাব সঠিকভাবে রাখুন।",
        "সঞ্চয়" to "লক্ষ্য ঠিক করে সঞ্চয় জমা করুন, অগ্রগতি দেখুন।",
        "ব্যাকআপ" to "নিজের ফোনেই ব্যাকআপ ফাইল রাখুন, প্রয়োজনে ফিরিয়ে আনুন।",
    )
}
