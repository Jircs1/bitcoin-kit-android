package io.horizontalsystems.bitcoinkit.transactions

import io.horizontalsystems.bitcoinkit.core.RealmFactory
import io.horizontalsystems.bitcoinkit.models.Transaction
import io.horizontalsystems.bitcoinkit.network.peer.PeerGroup
import io.horizontalsystems.bitcoinkit.transactions.builder.TransactionBuilder

class TransactionCreator(
        private val realmFactory: RealmFactory,
        private val builder: TransactionBuilder,
        private val processor: TransactionProcessor,
        private val peerGroup: PeerGroup) {

    fun create(address: String, value: Int, feeRate: Int, senderPay: Boolean) {
        val realm = realmFactory.realm
        val transaction = builder.buildTransaction(value, address, feeRate, senderPay, realm)

        check(realm.where(Transaction::class.java).equalTo("hashHexReversed", transaction.hashHexReversed).findFirst() == null) {
            throw TransactionAlreadyExists("hashHexReversed = ${transaction.hashHexReversed}")
        }

        realm.executeTransaction {
            realm.insert(transaction)
            processor.process(transaction, realm)
        }
        realm.close()

        peerGroup.sendPendingTransactions()
    }

    open class TransactionCreationException(msg: String) : Exception(msg)
    class TransactionAlreadyExists(msg: String) : TransactionCreationException(msg)

}
