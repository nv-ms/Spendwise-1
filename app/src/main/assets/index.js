const string1 = "RKS5N3G1WP Confirmed. Ksh20.00 sent to Safaricom Limited for account on 28/11/23 at 10:35 PM. New M-PESA balance is Ksh5,321.73. Transaction cost, Ksh0.00."
const string2 = "RKS4M5N6IK Confirmed. Ksh100.00 paid to WYCLIFF MONARI. on 28/11/23 at 6:30 PM.New M-PESA balance is Ksh5,381.73. Transaction cost, Ksh0.00. Amount you can transact within the day is 497,960.00. To move money from bank to M-PESA, dial *334#>Withdraw>From bank to MPESA"
const string3 = "RKT0ODTGYE Confirmed. Ksh100.00 sent to ETHEL  KIPKOECH 0706499188 on 29/11/23 at 1:12 PM. New M-PESA balance is Ksh5,221.73. Transaction cost, Ksh0.00. Amount you can transact within the day is 499,900.00. To reverse, foward this message to 456."
const string4 = "RKQ8GJX8MU confirmed.You bought Ksh7.00 of airtime on 26/11/23 at 10:40 PM.New M-PESA balance is Ksh4,425.73. Transaction cost, Ksh0.00. Amount you can transact within the day is 499,732.00. Dial *234*0# to Opt in to FULIZA and check your limit."
const string5 = "RKP5CABHA5 Confirmed. Ksh200.00 sent to KPLC PREPAID for account 14252449500 on 25/11/23 at 4:39 PM New M-PESA balance is Ksh3,701.73. Transaction cost, Ksh5.00.Amount you can transact within the day is 499,380.00. Pay your water/KPLC bill conveniently using M-PESA APP or use Paybill option on Lipa Na M-PESA."

function breakMessage(message) {
  const regex = /Ksh([\d,.]+).*Transaction cost, Ksh([\d,.]+)/i;
  const matches = message.match(regex);

  if (matches) {
    const amount = parseFloat(matches[1].replace(/,/g, ''));
    const transactionCost = parseFloat(matches[2].replace(/,/g, ''));
    const messageBody = {
      amount:amount,
      transactionCost:transactionCost
    }
    return messageBody;
  } else {
    return null;
  }
}

console.log(breakMessage(string1).amount)
