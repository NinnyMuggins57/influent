---
section: Docs
subsection: User Guide
chapter: Getting Started
topic: Recognizing Anomalous Patterns
permalink: docs/user-guide/getting-started/recognize-patterns/
layout: submenu
---

Recognizing Anomalous Patterns
==============================

Understanding expected transaction patterns in your source data is key to using Influent. Exceptions to these features may indicate suspicious behavior.

## <a name="flow-lines"></a> Transaction Flow Lines ##

Unexpected disparities in the transaction flow lines between accounts may indicate suspicious behavior.

**NOTE**: Make sure the [Transaction Flow Period](#transaction-flow-period) covers the entire time over which transactions were made between the accounts in question. Otherwise, fund transfer delays may suggest false positives.

<h6 class="procedure">To investigate transaction flow lines</h6>

1. Compare the flow lines between *Jason Walker's* checking account and its two destinations. 
2. Notice that the line going to his personal savings and cash accounts is a fraction of that going into the cluster of commercial accounts. <p class="list-paragraph">It is surprising that with such a large increase in income that his savings remain consistent. This may indicate suspicious or fraudulent behavior.</p>

## <a name="expansion"></a> Iterative Expansion ##

Now that we have identified one of *Jason Walker's* destinations for further investigation, we can iteratively expand it to detect any suspicious behavior it exhibits.

<h6 class="procedure">To investigate the cluster of commercial accounts</h6>

1. Expand the cluster to view its child accounts.
2. Note that the transaction flow lines indicate that the majority of funds are going to *NAGCG Consulting*.<img src="../../../../img/screenshots/workflow-nagcg.png" class="procedure-screenshot" alt="NAGCG Consulting" />
3. Review the *NAGCG's* activity chart. All of its incoming funds (above the x-axis) are orange, indicating that they came from *Jason Walker*, the [highlighted](../../how-to/investigate-flow/#highlighted) account. <p class="list-paragraph">This may indicate a shell organization.</p>
4. Hover your mouse over *NAGCG Consulting* and click the right **Branch** button to see where it sent funds.
5. Notice that there is only one destination: a personal checking account for *Helen Carter*.<img src="../../../../img/screenshots/workflow-hcarter.png" class="procedure-screenshot" alt="Helen Carter" /> *NAGCG Consulting* appears to have no real expenses.
6. To understand how much money *Helen Carter* received from *NAGCG Consulting*, hover your mouse over *NAGCG Consulting* and click the right **Highlight Flow** ![Remove button](../../../../img/screenshots/buttons/highlight.png) button. This shows all transactions with *NAGCG Consulting* in color.
7. *Helen Carter's* account activity chart seems to indicate normal levels of account activity until *NAGCG* transferred her four large sums of money.<img src="../../../../img/screenshots/workflow-nagcg-highlighted.png" class="procedure-screenshot" alt="NAGCG Consulting Highlighted" />

## <a name="transaction-records"></a> Detailed Transaction Records ##

Now we want to investigate the individual transactions received by *Helen Carter* to review the large fund transfers from *NAGCG*.

<h6 class="procedure">To investigate *Helen Carter's* transactions</h6>

1. Click the *Helen Carter* card to open the Details Pane.
2. Click the **View transactions for selected entity** button above the transaction table or chart.<img src="../../../../img/screenshots/workflow-send-flow-to-transactions.png" class="procedure-screenshot" alt="View Transactions for Selected Entity" />
3. The Transaction tab lists *Helen Carter's* transactions in detail.<img src="../../../../img/screenshots/workflow-transaction-tab.png" class="procedure-screenshot" alt="Helen Carter Transactions" />
4. Click the **Amount (USD)** column to sort *Helen Carter's* transactions in descending order of value.
5. Note that *Helen Carter's* largest transactions are cash withdrawals in excess of $100K.
6. To save *Helen Carter's* transactions to a CSV file, select **Export Transactions** from the **File** menu.

## Next Steps ##

Influent enables us to [export Flow workspace data](../export-data/) in a number of different ways.