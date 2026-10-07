// Tests the SMS parser inside index.html against real-world bank formats.
// Usage: node tools/test-sms.js   (add every new bank format you fix as a case below)
const fs = require('fs'), path = require('path'), vm = require('vm');
const html = fs.readFileSync(path.join(__dirname, '..', 'index.html'), 'utf8');
const start = html.indexOf('const BRANDS = ['), end = html.indexOf('let scanning = false;');
const ctx = { round2: v => Math.round(v * 100) / 100 };
vm.createContext(ctx);
vm.runInContext(html.slice(start, end) + '\nthis.parseSms = parseSms;', ctx);
const parseSms = ctx.parseSms;
// [sender, body, expected (null = must be ignored) as {amount, mode, title}]
const cases = [
  ['VM-HDFCBK', 'Sent Rs.250.00\nFrom HDFC Bank A/C *1234\nTo SWIGGY\nOn 05/10/24\nRef 428912345678\nNot You?\nCall 18002586161/SMS BLOCK UPI to 7308080808', { amount: 250, mode: 'UPI', title: 'Swiggy' }],
  ['AD-HDFCBK', 'Money Sent-INR 120.00 From HDFC Bank A/C x1234 on 05-10-24 To VPA rapido.rides@axisbank Ref-428912345679 Not you? Call 18002586161', { amount: 120, mode: 'UPI', title: 'Rapido' }],
  ['VM-HDFCBK', 'Spent Rs.1299 On HDFC Bank Card 5678 At AMAZON PAY INDIA On 2024-10-05:14:22:11 Bal Rs.45000 Not You? Call 18002586161/SMS BLOCK DC 5678 to 7308080808', { amount: 1299, mode: 'Card', title: 'Amazon' }],
  ['JD-ICICIB', 'ICICI Bank Acct XX123 debited for Rs 380.00 on 05-Oct-24; ZOMATO credited. UPI:428912345680. Call 18002662 for dispute. SMS BLOCK 123 to 9215676766.', { amount: 380, mode: 'UPI', title: 'Zomato' }],
  ['VK-ICICIB', 'INR 2,199.00 spent using ICICI Bank Card XX9876 on 05-Oct-24 on MYNTRA. Avl Limit: INR 1,20,000.00. If not you, call 1800 2662/SMS BLOCK 9876 to 9215676766', { amount: 2199, mode: 'Card', title: 'Myntra' }],
  ['AD-SBIUPI', 'Dear UPI user A/C X4321 debited by 60.0 on date 05Oct24 trf to CHAI POINT Refno 428912345681. If not u? call 1800111109. -SBI', { amount: 60, mode: 'UPI', title: 'Chai Point' }],
  ['AX-AXISBK', 'INR 500.00 debited\nA/c no. XX7890\n05-10-24, 14:22:11\nUPI/P2M/428912345682/BLINKIT\nNot you? SMS BLOCKUPI Cust ID to 919951860002\nAxis Bank', { amount: 500, mode: 'UPI', title: 'Blinkit' }],
  ['VM-KOTAKB', 'Sent Rs.1000.00 from Kotak Bank AC X5678 to rahul.mehta@okaxis on 05-10-24.UPI Ref 428912345683. Not you, https://kotak.com/KBANKT/Fraud', { amount: 1000, mode: 'UPI', title: 'Rahul Mehta' }],
  ['VM-PAYTMB', 'Rs.149 sent to Jio Recharge from Paytm Payments Bank a/c 91XXXX1234. UPI Ref: 428912345684.', { amount: 149, mode: 'UPI', title: 'Jio recharge' }],
  ['VM-HDFCBK', 'Rs.649.00 debited from A/c XX1234 for Netflix AutoPay. UPI Ref 428912345686', { amount: 649, mode: 'UPI', title: 'Netflix' }],
  ['BP-BOBTXN', 'Rs.150.00 Dr. from A/C XXXXXX1234 and Cr. to ola@ybl. Ref:428912345687. AvlBal:Rs12000.00(2024:10:05 14:22:11). Not you? Call 18005700-BOB', { amount: 150, mode: 'UPI', title: 'Ola' }],
  ['VM-PNBSMS', 'Your A/c XX1234 has been debited for INR 90.00 on 05-10-2024 15:01:10 through UPI. Avl Bal INR 8,500.00. UPI Ref 428912345688', { amount: 90, mode: 'UPI', title: 'UPI payment' }],
  ['VM-HDFCBK', 'Sent Rs.40.00\nFrom HDFC Bank A/C *1234\nTo 9876543210@ybl\nOn 05/10/24\nRef 428912345690', { amount: 40, mode: 'UPI', title: 'UPI payment' }],
  ['VM-HDFCBK', 'Spent Rs.450 On HDFC Bank Card 5678 At STARBUCKS On 2024-10-05. Get exciting offers on your card at hdfcbank.com/offers', { amount: 450, mode: 'Card', title: 'Starbucks' }],
  // must be ignored
  ['VM-HDFCBK', 'Rs.25000.00 credited to HDFC Bank A/c XX1234 on 01-10-24 from VPA employer@hdfc (UPI Ref No 428912345685)', null],
  ['JD-ICICIB', 'ICICI Bank Account XX123 credited:Rs. 500.00 on 05-Oct-24. Info NEFT-ABC. Available Balance is Rs. 10,000.00.', null],
  ['VM-HDFCBK', 'OTP for txn of Rs 1,299.00 at AMAZON on HDFC Bank card ending 5678 is 123456. Valid for 10 mins.', null],
  ['VM-HDFCBK', 'Get instant loan up to Rs 5,00,000! Apply now on HDFC Bank app.', null],
  ['VM-GPAYIN', 'Rahul has requested money of Rs 500 on Google Pay UPI app. On approving, amount will be debited from your account.', null],
  ['VM-HDFCBK', 'Your AutoPay of Rs 649 for Netflix will be debited on 07-10-24 from A/c XX1234.', null],
  ['VM-SBIINB', 'Rs.2000 withdrawn at SBI ATM S1234 from A/c XX4321 on 05Oct24. Avl Bal Rs 8000.', null],
  ['VM-AMZPAY', 'Refund of Rs 299 credited to your Amazon Pay balance for order 405-123.', null],
  ['+919812345678', 'Bhai I paid Rs 500 for dinner, send your share', null],
  ['VM-HDFCBK', 'Txn of Rs.300 to swiggy@ybl failed. Amount if debited will be reversed in 3 days.', null],
];
let pass = 0;
for (const [addr, body, want] of cases) {
  const got = parseSms(body, addr);
  const ok = want === null ? got === null : got && got.amount === want.amount && got.mode === want.mode && got.title === want.title;
  if (ok) pass++;
  console.log((ok ? 'PASS ' : 'FAIL ') + (want ? `${want.title} ${want.amount}` : 'ignore: ' + body.slice(0, 45)) + (ok ? '' : '  got ' + JSON.stringify(got)));
}
console.log(`${pass}/${cases.length} passed`);

