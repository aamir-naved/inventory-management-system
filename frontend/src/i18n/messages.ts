export type AppLocale = "en" | "hi";

export const LOCALE_STORAGE_KEY = "ims.locale";

type MessageKey =
  | "nav.counter"
  | "nav.sales"
  | "nav.customers"
  | "nav.dashboard"
  | "nav.products"
  | "nav.inventory"
  | "nav.purchases"
  | "nav.suppliers"
  | "nav.reports"
  | "nav.team"
  | "nav.audit"
  | "nav.business"
  | "nav.settings"
  | "nav.profile"
  | "nav.start"
  | "nav.fullSetup"
  | "nav.detail.barcodeSale"
  | "nav.detail.invoices"
  | "nav.detail.parties"
  | "nav.detail.pulse"
  | "nav.detail.catalog"
  | "nav.detail.stock"
  | "nav.detail.bills"
  | "nav.detail.insights"
  | "nav.detail.staff"
  | "nav.detail.log"
  | "nav.detail.workspace"
  | "nav.detail.prefs"
  | "nav.detail.account"
  | "nav.detail.shopName"
  | "nav.detail.gstLater"
  | "shell.brandKicker"
  | "shell.brandTitle"
  | "shell.brandBody"
  | "shell.welcome"
  | "shell.tagline"
  | "shell.alerts"
  | "shell.businessReady"
  | "shell.setupPending"
  | "shell.signOut"
  | "shell.menu"
  | "shell.closeMenu"
  | "shell.language"
  | "shell.noBusiness"
  | "pos.kicker"
  | "pos.title"
  | "pos.subtitle"
  | "pos.barcode"
  | "pos.barcodePlaceholder"
  | "pos.customer"
  | "pos.selectCustomer"
  | "pos.walkInNote"
  | "pos.amountPaid"
  | "pos.completeSale"
  | "pos.saving"
  | "pos.offlineTitle"
  | "pos.offlineBody"
  | "pos.remove"
  | "pos.total"
  | "pos.taxable"
  | "pos.tax"
  | "pos.emptyTitle"
  | "pos.openShop"
  | "pos.shareBill"
  | "pos.printBill"
  | "common.loading";

const en: Record<MessageKey, string> = {
  "nav.counter": "Counter",
  "nav.sales": "Sales",
  "nav.customers": "Customers",
  "nav.dashboard": "Dashboard",
  "nav.products": "Products",
  "nav.inventory": "Inventory",
  "nav.purchases": "Purchases",
  "nav.suppliers": "Suppliers",
  "nav.reports": "Reports",
  "nav.team": "Team",
  "nav.audit": "Activity",
  "nav.business": "Business",
  "nav.settings": "Settings",
  "nav.profile": "Profile",
  "nav.start": "Start",
  "nav.fullSetup": "Full setup",
  "nav.detail.barcodeSale": "Barcode sale",
  "nav.detail.invoices": "Invoices",
  "nav.detail.parties": "Parties",
  "nav.detail.pulse": "Quick pulse",
  "nav.detail.catalog": "Catalog",
  "nav.detail.stock": "Stock",
  "nav.detail.bills": "Bills",
  "nav.detail.insights": "Insights",
  "nav.detail.staff": "Staff",
  "nav.detail.log": "Log",
  "nav.detail.workspace": "Workspace",
  "nav.detail.prefs": "Prefs",
  "nav.detail.account": "Account",
  "nav.detail.shopName": "Shop name",
  "nav.detail.gstLater": "GST later",
  "shell.brandKicker": "Inventory for your shop",
  "shell.brandTitle": "Bill from the counter. Stock stays on the server.",
  "shell.brandBody":
    "Open this link on a phone. Sales, stock, and invoices live in the cloud — not on a shop PC that can be lost.",
  "shell.welcome": "Welcome back, {name}",
  "shell.tagline": "Sell first. Numbers and GST can wait until after the bill.",
  "shell.alerts": "Alerts ({count})",
  "shell.businessReady": "Business ready",
  "shell.setupPending": "Setup pending",
  "shell.signOut": "Sign out",
  "shell.menu": "Menu",
  "shell.closeMenu": "Close menu",
  "shell.language": "Language",
  "shell.noBusiness": "No business configured yet",
  "pos.kicker": "Counter",
  "pos.title": "Fast sale entry with barcode.",
  "pos.subtitle": "Scan or type a barcode, collect payment, then share or print the bill.",
  "pos.barcode": "Barcode",
  "pos.barcodePlaceholder": "Scan or type and press Enter",
  "pos.customer": "Customer",
  "pos.selectCustomer": "Select customer",
  "pos.walkInNote": "Walk-in is chosen automatically for cash sales.",
  "pos.amountPaid": "Amount paid",
  "pos.completeSale": "Complete sale",
  "pos.saving": "Saving...",
  "pos.offlineTitle": "You are offline",
  "pos.offlineBody":
    "Do not complete a sale until the connection returns. Bills will not save without the server.",
  "pos.remove": "Remove",
  "pos.total": "Total {amount}",
  "pos.taxable": "Taxable {amount}",
  "pos.tax": "Tax {amount}",
  "pos.emptyTitle": "Name the shop before using the counter.",
  "pos.openShop": "Open the shop",
  "pos.shareBill": "Share bill",
  "pos.printBill": "Print",
  "common.loading": "Loading…",
};

const hi: Record<MessageKey, string> = {
  "nav.counter": "काउंटर",
  "nav.sales": "बिक्री",
  "nav.customers": "ग्राहक",
  "nav.dashboard": "डैशबोर्ड",
  "nav.products": "उत्पाद",
  "nav.inventory": "स्टॉक",
  "nav.purchases": "खरीद",
  "nav.suppliers": "सप्लायर",
  "nav.reports": "रिपोर्ट",
  "nav.team": "टीम",
  "nav.audit": "गतिविधि",
  "nav.business": "व्यवसाय",
  "nav.settings": "सेटिंग्स",
  "nav.profile": "प्रोफ़ाइल",
  "nav.start": "शुरू",
  "nav.fullSetup": "पूरी सेटअप",
  "nav.detail.barcodeSale": "बारकोड बिक्री",
  "nav.detail.invoices": "बिल",
  "nav.detail.parties": "पार्टी",
  "nav.detail.pulse": "आज का सार",
  "nav.detail.catalog": "सूची",
  "nav.detail.stock": "स्टॉक",
  "nav.detail.bills": "बिल",
  "nav.detail.insights": "विवरण",
  "nav.detail.staff": "स्टाफ़",
  "nav.detail.log": "लॉग",
  "nav.detail.workspace": "दुकान",
  "nav.detail.prefs": "पसंद",
  "nav.detail.account": "खाता",
  "nav.detail.shopName": "दुकान का नाम",
  "nav.detail.gstLater": "GST बाद में",
  "shell.brandKicker": "आपकी दुकान का इन्वेंटरी",
  "shell.brandTitle": "काउंटर से बिल। स्टॉक सर्वर पर सुरक्षित।",
  "shell.brandBody":
    "फ़ोन पर यह लिंक खोलें। बिक्री, स्टॉक और बिल क्लाउड में रहते हैं — खो जाने वाले कंप्यूटर पर नहीं।",
  "shell.welcome": "नमस्ते, {name}",
  "shell.tagline": "पहले बेचें। बिल के बाद नंबर और GST।",
  "shell.alerts": "अलर्ट ({count})",
  "shell.businessReady": "दुकान तैयार",
  "shell.setupPending": "सेटअप बाकी",
  "shell.signOut": "साइन आउट",
  "shell.menu": "मेनू",
  "shell.closeMenu": "मेनू बंद",
  "shell.language": "भाषा",
  "shell.noBusiness": "अभी कोई दुकान नहीं",
  "pos.kicker": "काउंटर",
  "pos.title": "बारकोड से तेज़ बिक्री।",
  "pos.subtitle": "बारकोड स्कैन या टाइप करें, पैसे लें, फिर बिल शेयर या प्रिंट करें।",
  "pos.barcode": "बारकोड",
  "pos.barcodePlaceholder": "स्कैन करें या टाइप कर Enter दबाएँ",
  "pos.customer": "ग्राहक",
  "pos.selectCustomer": "ग्राहक चुनें",
  "pos.walkInNote": "नकद बिक्री के लिए वॉक-इन अपने आप चुना जाता है।",
  "pos.amountPaid": "अभी मिला",
  "pos.completeSale": "बिक्री पूरी करें",
  "pos.saving": "सेव हो रहा है...",
  "pos.offlineTitle": "आप ऑफ़लाइन हैं",
  "pos.offlineBody":
    "कनेक्शन आने तक बिक्री पूरी न करें। सर्वर के बिना बिल सेव नहीं होगा।",
  "pos.remove": "हटाएँ",
  "pos.total": "कुल {amount}",
  "pos.taxable": "कर योग्य {amount}",
  "pos.tax": "टैक्स {amount}",
  "pos.emptyTitle": "काउंटर से पहले दुकान का नाम दें।",
  "pos.openShop": "दुकान खोलें",
  "pos.shareBill": "बिल शेयर",
  "pos.printBill": "प्रिंट",
  "common.loading": "लोड हो रहा है…",
};

const catalogs: Record<AppLocale, Record<MessageKey, string>> = { en, hi };

export function translate(
  locale: AppLocale,
  key: MessageKey,
  vars?: Record<string, string | number>,
) {
  let text = catalogs[locale][key] ?? catalogs.en[key] ?? key;
  if (vars) {
    for (const [name, value] of Object.entries(vars)) {
      text = text.split(`{${name}}`).join(String(value));
    }
  }
  return text;
}

export type { MessageKey };
