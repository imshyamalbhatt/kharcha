package com.shyamal.kharcha;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plain-Java check used by SmsReceiver: is this incoming SMS a bank / UPI spend, and for how much?
 * Mirrors SMS_SKIP / SMS_DEBIT / SMS_CREDIT_TO_ME / smsAmount in index.html: keep them in step.
 * No Android imports, so it can be tested on a laptop (tools/test-sms-native.ps1).
 */
public final class SmsRules {
    private SmsRules() {}

    private static final Pattern SKIP = Pattern.compile("(?i)\\botp\\b|one.?time.?pass|verification code|will be debited|to be debited|due (on|date|by)|is due|requested (money|rs|inr)|has requested|collect request|autopay of .{0,40}(will|scheduled)|mandate (is )?(registered|created|set)|declined|\\bfailed\\b|unsuccessful|reversed|reversal|\\batm\\b|cash withdrawal|withdrawn|pre-?approved|apply now|loan (of|up ?to)");
    private static final Pattern DEBIT = Pattern.compile("(?i)debited|\\bspent\\b|\\bsent\\b|money sent|\\bpaid\\b|\\bdr\\.?(?=\\s|$)|deducted|\\btxn (of )?(rs|inr)|\\bpurchase\\b|trf to");
    private static final Pattern CREDIT_TO_ME = Pattern.compile("(?i)credited to (your )?([a-z]+ )?(bank )?(a/?c|ac|acct|account)|(a/?c|acct|account)[^.;]{0,40}\\bcredited\\b|\\breceived\\b|\\brefund|cashback");
    private static final Pattern STRONG_DEBIT = Pattern.compile("(?i)debited|\\bspent\\b|\\bdr\\.?(?=\\s|$)|money sent|\\bsent rs");
    private static final Pattern BALANCE = Pattern.compile("(?i)(avl\\.?|avail\\.?|available|avbl\\.?)\\s*(bal(ance)?|lmt|limit)\\s*[:\\-]?\\s*(rs\\.?|inr|₹)?\\s*[\\d,]+(\\.\\d+)?");
    private static final Pattern BALANCE2 = Pattern.compile("(?i)\\b(bal(ance)?|limit)\\s*[:\\-]?\\s*(rs\\.?|inr|₹)\\s*[\\d,]+(\\.\\d+)?");
    private static final Pattern AMOUNT = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*:?\\s*([\\d,]+(?:\\.\\d{1,2})?)");
    private static final Pattern AMOUNT_SBI = Pattern.compile("(?i)debited (?:by|for|with)\\s*([\\d,]+(?:\\.\\d{1,2})?)");

    /** The debit amount as written in the SMS (e.g. "1,299.00"), or null if it isn't a spend. */
    public static String spendAmount(String from, String body) {
        if (from == null || body == null) return null;
        if (from.replaceAll("\\s", "").matches("\\+?\\d{6,}")) return null; // personal numbers
        if (SKIP.matcher(body).find() || !DEBIT.matcher(body).find()) return null;
        if (CREDIT_TO_ME.matcher(body).find() && !STRONG_DEBIT.matcher(body).find()) return null;
        String b = BALANCE2.matcher(BALANCE.matcher(body).replaceAll(" ")).replaceAll(" ");
        Matcher m = AMOUNT.matcher(b);
        if (m.find()) return m.group(1);
        m = AMOUNT_SBI.matcher(b);
        return m.find() ? m.group(1) : null;
    }
}
