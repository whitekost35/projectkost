package com.kost.project.data.repository

import android.util.Log
import com.kost.project.SupabaseClientInstance
import com.kost.project.data.local.TransactionDao
import com.kost.project.data.model.Transaction
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun insert(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
        try {
            withContext(Dispatchers.IO) {
                SupabaseClientInstance.client.from("transactions").upsert(transaction)
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to insert transaction: ${e.message}", e)
        }
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
        try {
            withContext(Dispatchers.IO) {
                SupabaseClientInstance.client.from("transactions").upsert(transaction)
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to update transaction: ${e.message}", e)
        }
    }

    suspend fun delete(transactionId: String) {
        transactionDao.deleteById(transactionId)
        try {
            withContext(Dispatchers.IO) {
                SupabaseClientInstance.client.from("transactions").delete {
                    filter {
                        eq("id", transactionId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to delete transaction: ${e.message}", e)
        }
    }

    suspend fun getCount(): Int {
        return transactionDao.getCount()
    }

    suspend fun insertAll(transactions: List<Transaction>) {
        transactionDao.insertAll(transactions)
        try {
            withContext(Dispatchers.IO) {
                SupabaseClientInstance.client.from("transactions").upsert(transactions)
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to insert all transactions: ${e.message}", e)
        }
    }

    suspend fun fetchFromCloud() {
        try {
            withContext(Dispatchers.IO) {
                val cloudTxs = SupabaseClientInstance.client.from("transactions")
                    .select()
                    .decodeList<Transaction>()
                if (cloudTxs.isNotEmpty()) {
                    transactionDao.insertAll(cloudTxs)
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to fetch transactions: ${e.message}", e)
            throw e
        }
    }

    suspend fun pushLocalToCloud() {
        try {
            val localTxs = transactionDao.getAllTransactions().first()
            if (localTxs.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    SupabaseClientInstance.client.from("transactions").upsert(localTxs)
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSync", "Failed to push transactions: ${e.message}", e)
            throw e
        }
    }
}
