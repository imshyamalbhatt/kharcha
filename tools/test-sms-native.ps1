# Tests android/native/SmsRules.java (the instant-alert check that runs even when the app is
# closed) against the same SMS cases as tools/test-sms.js. Needs the portable JDK.
# Usage: powershell -File tools/test-sms-native.ps1
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$jdk = "C:\Users\bhatt\android-build\jdk-21.0.12.1+1\bin"
$work = Join-Path $env:TEMP 'pm-sms-native-test'
Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "$work\com\shyamal\kharcha" | Out-Null

# Build Java test cases from tools/test-sms.js: [sender, body, expected amount or null]
$cases = node -e @'
const src = require('fs').readFileSync(process.argv[1], 'utf8');
const arr = src.slice(src.indexOf('const cases = ['), src.indexOf('];', src.indexOf('const cases = [')) + 2).replace('const cases =', '');
const cases = eval(arr);
const j = s => JSON.stringify(s);
console.log(cases.map(([a, b, w]) => `      {${j(a)}, ${j(b)}, ${w ? j(String(w.amount)) : 'null'}},`).join('\n'));
'@ "$repo\tools\test-sms.js"

$test = @"
package com.shyamal.kharcha;
public class SmsRulesTest {
  public static void main(String[] a) {
    String[][] cases = {
$($cases -join "`n")
    };
    int pass = 0;
    for (String[] c : cases) {
      String got = SmsRules.spendAmount(c[0], c[1]);
      Double g = got == null ? null : Double.valueOf(got.replace(",", ""));
      boolean ok = c[2] == null ? g == null : g != null && Math.abs(g - Double.parseDouble(c[2])) < 0.001;
      if (ok) pass++; else System.out.println("FAIL " + c[1].substring(0, Math.min(50, c[1].length())) + " -> " + got);
    }
    System.out.println(pass + "/" + cases.length + " passed (native)");
    if (pass != cases.length) System.exit(1);
  }
}
"@
[IO.File]::WriteAllText("$work\com\shyamal\kharcha\SmsRulesTest.java", $test, (New-Object System.Text.UTF8Encoding($false))) # no BOM: javac rejects it
Copy-Item "$repo\android\native\SmsRules.java" "$work\com\shyamal\kharcha\"
& "$jdk\javac.exe" -encoding UTF-8 -d "$work\out" "$work\com\shyamal\kharcha\SmsRules.java" "$work\com\shyamal\kharcha\SmsRulesTest.java"
& "$jdk\java.exe" -cp "$work\out" com.shyamal.kharcha.SmsRulesTest
