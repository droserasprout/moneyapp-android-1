package com.cactusteam.money.sync

import android.support.v4.util.ArrayMap
import com.cactusteam.money.data.DataConstants
import com.cactusteam.money.data.DataManager
import com.cactusteam.money.data.dao.*
import com.cactusteam.money.sync.changes.ChangeItem
import com.cactusteam.money.sync.changes.ChangesList
import com.cactusteam.money.sync.model.*
import java.util.*

/**
 * @author vpotapenko
 */
internal class SyncProxyDatabase(private val dataManager: DataManager) : IProxyDatabase {

    override fun runInTx(r: Runnable) {
        dataManager.daoSession.runInTx(r)
    }

    override fun alreadyApplied(type: Int, commandId: Long): Boolean {
        val count = dataManager.daoSession.syncLogDao.queryBuilder().where(SyncLogDao.Properties.Type.eq(type),
                SyncLogDao.Properties.GlobalId.eq(commandId)).limit(1).count()
        return count > 0
    }

    override fun clearDirties(items: List<ChangeItem>) {
        for (item in items) {
            clearDirty(item)

            val log = SyncLog()
            log.globalId = item.id
            log.type = item.objectWrapper.type
            dataManager.daoSession.insert(log)
        }
    }

    override fun createAccount(commandId: Long, a: SyncAccount) {
        dataManager.accountService.createAccountInternal(
                a.name,
                a.type,
                a.currencyCode,
                a.color,
                a.skipInBalance,
                a.globalId,
                true
        )

        createSyncLog(SyncConstants.ACCOUNT_TYPE, commandId)
    }

    private fun createSyncLog(type: Int, commandId: Long) {
        val log = SyncLog()
        log.globalId = commandId
        log.type = type
        dataManager.daoSession.insert(log)
    }

    override fun updateAccount(commandId: Long, a: SyncAccount) {
        val account = getAccountByGlobalId(a.globalId)
        if (account != null) {
            updateAccount(commandId, a, account)
        } else {
            // TODO added info message
        }
    }

    private fun updateAccount(commandId: Long, a: SyncAccount, account: Account) {
        dataManager.accountService.updateAccountInternal(
                account.id,
                a.name,
                a.type,
                a.currencyCode,
                a.color,
                a.skipInBalance,
                a.globalId,
                true
        )
        createSyncLog(SyncConstants.ACCOUNT_TYPE, commandId)
    }

    private fun getAccountByGlobalId(globalId: Long?): Account? {
        if (globalId == null) return null

        val list = dataManager.daoSession.accountDao.queryBuilder().where(AccountDao.Properties.GlobalId.eq(globalId)).list()
        return if (list.isEmpty()) null else list[0]
    }

    override fun deleteAccount(commandId: Long, a: SyncAccount) {
        val account = getAccountByGlobalId(a.globalId)
        if (account != null) dataManager.daoSession.delete(account)

        createSyncLog(SyncConstants.ACCOUNT_TYPE, commandId)
    }

    override fun mergeAccount(commandId: Long, a: SyncAccount, sourceId: Long?) {
        val account = dataManager.daoSession.accountDao.load(sourceId)
        if (account != null) {
            updateAccount(commandId, a, account)
        } else {
            createAccount(commandId, a)
        }
    }

    override fun getDirtyAccounts(): List<SyncAccount> {
        val daoSession = dataManager.daoSession
        val list = daoSession.accountDao.queryBuilder().whereOr(AccountDao.Properties.Synced.eq(false), AccountDao.Properties.GlobalId.isNull).list()
        val accounts = ArrayList<SyncAccount>()
        for (a in list) {
            val account = SyncAccount()
            account.globalId = if (a.globalId == null) -1 else a.globalId
            account.localId = a.id!!
            account.name = a.name
            account.color = a.color
            account.currencyCode = a.currencyCode
            account.deleted = a.deleted
            account.type = a.type
            account.skipInBalance = a.skipInBalance

            accounts.add(account)
        }

        val trashList = daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(SyncConstants.ACCOUNT_TYPE)).list()
        for (trash in trashList) {
            val account = SyncAccount()
            account.removed = true
            account.globalId = trash.globalId

            accounts.add(account)
        }
        return accounts
    }

    override fun hasDirtyAccounts(): Boolean {
        val daoSession = dataManager.daoSession
        val updatedCount = daoSession.accountDao.queryBuilder().whereOr(AccountDao.Properties.Synced.eq(false), AccountDao.Properties.GlobalId.isNull).count()

        return updatedCount > 0 || daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(SyncConstants.ACCOUNT_TYPE)).count() > 0

    }

    override fun createCategory(commandId: Long, c: SyncCategory) {
        dataManager.categoryService.createCategoryInternal(
                c.type,
                c.name,
                c.icon,
                c.globalId,
                true
        )
        createSyncLog(SyncConstants.CATEGORY_TYPE, commandId)
    }

    override fun updateCategory(commandId: Long, c: SyncCategory) {
        val category = getCategoryByGlobalId(c.globalId)
        if (category != null) {
            updateCategory(commandId, c, category)
        } else {
            // TODO added info log
        }
    }

    private fun updateCategory(commandId: Long, c: SyncCategory, category: Category) {
        dataManager.categoryService.updateCategoryInternal(
                category.id,
                c.name,
                c.icon,
                c.globalId,
                true
        )
        createSyncLog(SyncConstants.CATEGORY_TYPE, commandId)
    }

    private fun getCategoryByGlobalId(globalId: Long?): Category? {
        if (globalId == null) return null

        val list = dataManager.daoSession.categoryDao.queryBuilder().where(CategoryDao.Properties.GlobalId.eq(globalId)).list()
        return if (list.isEmpty()) null else list[0]
    }

    override fun deleteCategory(commandId: Long, c: SyncCategory) {
        val category = getCategoryByGlobalId(c.globalId)
        if (category != null) dataManager.daoSession.delete(category)

        createSyncLog(SyncConstants.CATEGORY_TYPE, commandId)
    }

    override fun mergeCategory(commandId: Long, c: SyncCategory, sourceId: Long?) {
        val category = dataManager.daoSession.categoryDao.load(sourceId)
        if (category != null) {
            updateCategory(commandId, c, category)
        } else {
            createCategory(commandId, c)
        }
    }

    override fun getDirtyCategories(): List<SyncCategory> {
        val daoSession = dataManager.daoSession
        val list = daoSession.categoryDao.queryBuilder().whereOr(CategoryDao.Properties.Synced.eq(false), CategoryDao.Properties.GlobalId.isNull).list()
        val categories = ArrayList<SyncCategory>()
        for (c in list) {
            val category = SyncCategory()
            category.globalId = if (c.globalId == null) -1 else c.globalId
            category.localId = c.id!!
            category.type = c.type
            category.name = c.name
            category.icon = c.icon
            category.deleted = c.deleted

            categories.add(category)
        }

        val trashList = daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(SyncConstants.CATEGORY_TYPE)).list()
        for (trash in trashList) {
            val category = SyncCategory()
            category.removed = true
            category.globalId = trash.globalId

            categories.add(category)
        }
        return categories
    }

    override fun hasDirtyCategories(): Boolean {
        val daoSession = dataManager.daoSession
        val updatedCount = daoSession.categoryDao.queryBuilder().whereOr(CategoryDao.Properties.Synced.eq(false), CategoryDao.Properties.GlobalId.isNull).count()
        return updatedCount > 0 || daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(SyncConstants.CATEGORY_TYPE)).count() > 0
    }

    override fun createSubcategory(commandId: Long, s: SyncSubcategory) {
        val category = getCategoryByGlobalId(s.globalCategoryId)
        if (category != null) {
            dataManager.categoryService.createSubcategoryInternal(
                    category.id,
                    s.name,
                    s.globalId,
                    true
            )
        }
        createSyncLog(SyncConstants.SUBCATEGORY_TYPE, commandId)
    }

    override fun updateSubcategory(commandId: Long, s: SyncSubcategory) {
        val subcategory = getSubcategoryByGlobalId(s.globalId)
        if (subcategory != null) {
            updateSubcategory(commandId, s, subcategory)
        } else {
            // TODO info
        }
    }

    private fun updateSubcategory(commandId: Long, s: SyncSubcategory, subcategory: Subcategory) {
        dataManager.categoryService.updateSubcategoryInternal(
                subcategory.id,
                s.name,
                s.globalId,
                true
        )
        createSyncLog(SyncConstants.SUBCATEGORY_TYPE, commandId)
    }

    private fun getSubcategoryByGlobalId(globalId: Long?): Subcategory? {
        if (globalId == null) return null

        val list = dataManager.daoSession.subcategoryDao.queryBuilder().where(SubcategoryDao.Properties.GlobalId.eq(globalId)).list()
        return if (list.isEmpty()) null else list[0]
    }

    override fun deleteSubcategory(commandId: Long, s: SyncSubcategory) {
        val subcategory = getSubcategoryByGlobalId(s.globalId)
        if (subcategory != null) dataManager.daoSession.delete(subcategory)

        createSyncLog(SyncConstants.SUBCATEGORY_TYPE, commandId)
    }

    override fun mergeSubcategory(commandId: Long, s: SyncSubcategory, sourceId: Long?) {
        val subcategory = dataManager.daoSession.subcategoryDao.load(sourceId)
        if (subcategory != null) {
            updateSubcategory(commandId, s, subcategory)
        } else {
            createSubcategory(commandId, s)
        }
    }

    override fun getDirtySubcategories(): List<SyncSubcategory> {
        val daoSession = dataManager.daoSession
        val list = daoSession.subcategoryDao.queryBuilder().whereOr(SubcategoryDao.Properties.Synced.eq(false), SubcategoryDao.Properties.GlobalId.isNull).list()
        val subcategories = ArrayList<SyncSubcategory>()
        for (s in list) {
            val subcategory = SyncSubcategory()
            subcategory.globalId = if (s.globalId == null) -1 else s.globalId
            subcategory.localId = s.id!!
            subcategory.name = s.name
            subcategory.deleted = s.deleted
            subcategory.globalCategoryId = s.category.globalId!!

            subcategories.add(subcategory)
        }

        val trashList = daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(SyncConstants.SUBCATEGORY_TYPE)).list()
        for (trash in trashList) {
            val subcategory = SyncSubcategory()
            subcategory.removed = true
            subcategory.globalId = trash.globalId

            subcategories.add(subcategory)
        }
        return subcategories
    }

    override fun hasDirtySubcategories(): Boolean {
        val daoSession = dataManager.daoSession
        val updatedCount = daoSession.subcategoryDao.queryBuilder().whereOr(SubcategoryDao.Properties.Synced.eq(false), SubcategoryDao.Properties.GlobalId.isNull).count()
        return updatedCount > 0 || daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(SyncConstants.SUBCATEGORY_TYPE)).count() > 0
    }

    override fun createTransaction(commandId: Long, t: SyncTransaction) {
        val b = dataManager.transactionService
                .newTransactionBuilder()
                .putGlobalId(t.globalId)
                .putSynced(true)
                .putType(t.type)
                .putDate(Date(t.date))
        val account = getAccountByGlobalId(t.globalSourceAccountId)
        b.putSourceAccountId(if (account != null) account.id else 0)
                .putComment(t.comment)
                .putRef(convertToLocalRef(t.ref))
                .putStatus(t.status)
                .putAmount(t.amount)

        if (t.globalCategoryId != null) {
            val category = getCategoryByGlobalId(t.globalCategoryId)
            b.putCategoryId(category?.id)
        }
        if (t.globalSubcategoryId != null) {
            val subcategory = getSubcategoryByGlobalId(t.globalSubcategoryId)
            b.putSubcategoryId(subcategory?.id)
        }
        if (t.globalDestAccountId != null) {
            val destAccount = getAccountByGlobalId(t.globalDestAccountId)
            b.putDestAccountId(destAccount?.id)
        }
        b.putDestAmount(t.destAmount)

        for (tag in t.tags) {
            b.putTag(tag)
        }
        b.createInternal()
        createSyncLog(SyncConstants.TRANSACTION_TYPE, commandId)
    }

    override fun updateTransaction(commandId: Long, t: SyncTransaction) {
        if (!isDeletedObject(SyncConstants.TRANSACTION_TYPE, t.globalId)) {
            val transaction = getTransactionByGlobalId(t.globalId)
            if (transaction != null) {
                updateTransaction(commandId, t, transaction)
            } else {
                // TODO info
            }
        } else {
            createSyncLog(SyncConstants.TRANSACTION_TYPE, commandId)
        }
    }

    private fun updateTransaction(commandId: Long, t: SyncTransaction, transaction: Transaction) {
        val b = dataManager.transactionService.newTransactionBuilder()
                .putId(transaction.id)
                .putGlobalId(t.globalId)
                .putSynced(true)
                .putDate(Date(t.date))
        val account = getAccountByGlobalId(t.globalSourceAccountId)
        b.putSourceAccountId(if (account != null) account.id else 0)
                .putComment(t.comment)
                .putRef(convertToLocalRef(t.ref))
                .putStatus(t.status)
                .putAmount(t.amount)

        if (t.globalCategoryId != null) {
            val category = getCategoryByGlobalId(t.globalCategoryId)
            b.putCategoryId(category?.id)
        }
        if (t.globalSubcategoryId != null) {
            val subcategory = getSubcategoryByGlobalId(t.globalSubcategoryId)
            b.putSubcategoryId(subcategory?.id)
        }
        if (t.globalDestAccountId != null) {
            val destAccount = getAccountByGlobalId(t.globalDestAccountId)
            b.putDestAccountId(destAccount?.id)
        }
        b.putDestAmount(t.destAmount)

        for (tag in t.tags) {
            b.putTag(tag)
        }
        b.updateInternal()
        createSyncLog(SyncConstants.TRANSACTION_TYPE, commandId)
    }

    private fun getTransactionByGlobalId(globalId: Long): Transaction? {
        val list = dataManager.daoSession.transactionDao.queryBuilder().where(TransactionDao.Properties.GlobalId.eq(globalId)).list()
        return if (list.isEmpty()) null else list[0]
    }

    override fun deleteTransaction(commandId: Long, t: SyncTransaction) {
        if (!isDeletedObject(SyncConstants.TRANSACTION_TYPE, t.globalId)) {
            val transaction = getTransactionByGlobalId(t.globalId)
            if (transaction != null) dataManager.daoSession.delete(transaction)
        } else {
            clearTrash(SyncConstants.TRANSACTION_TYPE, t.globalId)
        }

        createSyncLog(SyncConstants.TRANSACTION_TYPE, commandId)
    }

    override fun mergeTransaction(commandId: Long, t: SyncTransaction, sourceId: Long?) {
        val transaction = dataManager.daoSession.transactionDao.load(sourceId)
        if (transaction != null) {
            updateTransaction(commandId, t, transaction)
        } else {
            createTransaction(commandId, t)
        }
    }

    override fun getDirtyTransactions(): List<SyncTransaction> {
        val daoSession = dataManager.daoSession
        val list = daoSession.transactionDao.queryBuilder().whereOr(TransactionDao.Properties.Synced.eq(false), TransactionDao.Properties.GlobalId.isNull).list()
        val transactions = ArrayList<SyncTransaction>()
        for (t in list) {
            val transaction = SyncTransaction()
            transaction.globalId = if (t.globalId == null) -1 else t.globalId
            transaction.localId = t.id!!
            transaction.type = t.type
            transaction.date = t.date.time
            transaction.globalSourceAccountId = t.sourceAccount.globalId!!
            transaction.comment = t.comment
            transaction.ref = convertToGlobalRef(t.ref)
            transaction.status = t.status
            transaction.amount = t.amount

            transaction.globalCategoryId = if (t.categoryId != null) t.category.globalId else null
            transaction.globalSubcategoryId = if (t.subcategoryId != null) t.subcategory.globalId else null
            transaction.globalDestAccountId = if (t.destAccountId != null) t.destAccount.globalId else null
            transaction.destAmount = t.destAmount

            for (tag in t.tags) {
                transaction.tags.add(tag.tag.name)
            }


            transactions.add(transaction)
        }

        val trashList = daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(SyncConstants.TRANSACTION_TYPE)).list()
        for (trash in trashList) {
            val transaction = SyncTransaction()
            transaction.removed = true
            transaction.globalId = trash.globalId

            transactions.add(transaction)
        }
        return transactions
    }

    override fun hasDirtyTransactions(): Boolean {
        val daoSession = dataManager.daoSession
        val updatedCount = daoSession.transactionDao.queryBuilder().whereOr(TransactionDao.Properties.Synced.eq(false), TransactionDao.Properties.GlobalId.isNull).count()
        return updatedCount > 0 || daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(SyncConstants.TRANSACTION_TYPE)).count() > 0
    }

    private fun convertToLocalRef(ref: String?): String? {
        if (ref != null && ref.startsWith(Debt.DEBT_REF_START)) { // Debt reference
            val debtId = ref.substring(Debt.DEBT_REF_START.length)

            val debt = getDebtByGlobalId(java.lang.Long.parseLong(debtId))
            return if (debt == null) null else String.format(Debt.DEBT_REF_PATTERN, debt.id)
        } else {
            return ref
        }
    }

    private fun getDebtByGlobalId(globalId: Long): Debt? {
        val list = dataManager.daoSession.debtDao.queryBuilder().where(DebtDao.Properties.GlobalId.eq(globalId)).list()
        return if (list.isEmpty()) null else list[0]
    }

    private fun getDebtNoteByGlobalId(globalId: Long): DebtNote? {
        val list = dataManager.daoSession.debtNoteDao.queryBuilder().where(DebtNoteDao.Properties.GlobalId.eq(globalId)).list()
        return if (list.isEmpty()) null else list[0]
    }

    private fun convertToGlobalRef(ref: String?): String? {
        if (ref != null && ref.startsWith(Debt.DEBT_REF_START)) { // Debt reference
            val debtId = ref.substring(Debt.DEBT_REF_START.length)

            val debt = dataManager.daoSession.debtDao.load(java.lang.Long.parseLong(debtId))
            return String.format(Debt.DEBT_REF_PATTERN, debt.globalId)
        } else {
            return ref
        }
    }

    override fun createDebt(commandId: Long, d: SyncDebt) {
        val b = dataManager.debtService
                .newDebtBuilder()
                .putGlobalId(d.globalId)
                .putSynced(true)
                .putName(d.name)
                .putPhone(d.phone)
                .putFinished(d.finished)

        val account = getAccountByGlobalId(d.globalAccountId)
        if (account != null) b.putAccountId(account.id)
        if (d.till != null) b.putTill(Date(d.till))
        if (d.start != null) b.putStart(Date(d.start))

        b.createInternal()

        createSyncLog(SyncConstants.DEBT_TYPE, commandId)
    }

    override fun updateDebt(commandId: Long, d: SyncDebt) {
        if (!isDeletedObject(SyncConstants.DEBT_TYPE, d.globalId)) {
            val debt = getDebtByGlobalId(d.globalId)
            if (debt != null) {
                updateDebt(commandId, d, debt)
            } else {
                // TODO info
            }
        } else {
            createSyncLog(SyncConstants.DEBT_TYPE, commandId)
        }
    }

    private fun updateDebt(commandId: Long, d: SyncDebt, debt: Debt) {
        val b = dataManager.debtService
                .newDebtBuilder()
                .putId(debt.id)
                .putGlobalId(d.globalId)
                .putSynced(true)
                .putName(d.name)
                .putPhone(d.phone)
                .putFinished(d.finished)
        if (d.till != null) b.putTill(Date(d.till))
        if (d.start != null) b.putStart(Date(d.start))

        val account = getAccountByGlobalId(d.globalAccountId)
        if (account != null) b.putAccountId(account.id)

        b.updateInternal()
        createSyncLog(SyncConstants.DEBT_TYPE, commandId)
    }

    override fun deleteDebt(commandId: Long, d: SyncDebt) {
        if (!isDeletedObject(SyncConstants.DEBT_TYPE, d.globalId)) {
            val debt = getDebtByGlobalId(d.globalId)
            if (debt != null) dataManager.daoSession.delete(debt)
        } else {
            clearTrash(SyncConstants.DEBT_TYPE, d.globalId)
        }

        createSyncLog(SyncConstants.DEBT_TYPE, commandId)
    }

    override fun mergeDebt(commandId: Long, d: SyncDebt, sourceId: Long?) {
        val debt = dataManager.daoSession.debtDao.load(sourceId)
        if (debt != null) {
            updateDebt(commandId, d, debt)
        } else {
            createDebt(commandId, d)
        }
    }

    override fun getDirtyDebts(): List<SyncDebt> {
        val daoSession = dataManager.daoSession
        val list = daoSession.debtDao.queryBuilder().whereOr(DebtDao.Properties.Synced.eq(false), DebtDao.Properties.GlobalId.isNull).list()
        val debts = ArrayList<SyncDebt>()
        for (d in list) {
            val debt = SyncDebt()
            debt.globalId = if (d.globalId == null) -1 else d.globalId
            debt.localId = d.id!!
            debt.name = d.name
            debt.phone = d.phone
            debt.finished = d.finished
            debt.till = d.till?.time
            debt.start = d.start?.time
            debt.globalAccountId = d.account.globalId!!

            debts.add(debt)
        }

        val trashList = daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(SyncConstants.DEBT_TYPE)).list()
        for (trash in trashList) {
            val debt = SyncDebt()
            debt.removed = true
            debt.globalId = trash.globalId

            debts.add(debt)
        }
        return debts
    }

    override fun hasDirtyDebts(): Boolean {
        val daoSession = dataManager.daoSession
        val updatedCount = daoSession.debtDao.queryBuilder().whereOr(DebtDao.Properties.Synced.eq(false), DebtDao.Properties.GlobalId.isNull).count()
        return updatedCount > 0 || daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(SyncConstants.DEBT_TYPE)).count() > 0
    }

    override fun createDebtNote(commandId: Long, n: SyncDebtNote) {
        val debt = getDebtByGlobalId(n.globalDebtId)
        if (debt != null) {
            val note = dataManager.debtService.createDebtNoteInternal(debt.id, n.text, false)
            updateDebtNote(commandId, n, note)
        } else {
            createSyncLog(SyncConstants.DEBT_NOTE_TYPE, commandId)
        }
    }

    private fun updateDebtNote(commandId: Long, n: SyncDebtNote, note: DebtNote) {
        note.date = Date(n.date ?: System.currentTimeMillis())
        note.text = n.text
        note.globalId = n.globalId
        note.synced = true
        dataManager.daoSession.update(note)

        createSyncLog(SyncConstants.DEBT_NOTE_TYPE, commandId)
    }

    override fun updateDebtNote(commandId: Long, n: SyncDebtNote) {
        if (!isDeletedObject(SyncConstants.DEBT_NOTE_TYPE, n.globalId)) {
            val debtNote = getDebtNoteByGlobalId(n.globalId)
            if (debtNote != null) {
                updateDebtNote(commandId, n, debtNote)
            } else {
                // TODO info
            }
        } else {
            createSyncLog(SyncConstants.DEBT_NOTE_TYPE, commandId)
        }
    }

    override fun deleteDebtNote(commandId: Long, n: SyncDebtNote) {
        if (!isDeletedObject(SyncConstants.DEBT_NOTE_TYPE, n.globalId)) {
            val debtNote = getDebtNoteByGlobalId(n.globalId)
            if (debtNote != null) dataManager.daoSession.delete(debtNote)
        } else {
            clearTrash(SyncConstants.DEBT_NOTE_TYPE, n.globalId)
        }

        createSyncLog(SyncConstants.DEBT_NOTE_TYPE, commandId)
    }

    override fun mergeDebtNote(commandId: Long, n: SyncDebtNote, sourceId: Long?) {
        val debtNote = dataManager.daoSession.debtNoteDao.load(sourceId)
        if (debtNote != null) {
            updateDebtNote(commandId, n, debtNote)
        } else {
            createDebtNote(commandId, n)
        }
    }

    override fun getDirtyDebtNotes(): MutableList<SyncDebtNote> {
        val daoSession = dataManager.daoSession
        val list = daoSession.debtNoteDao.queryBuilder().whereOr(DebtNoteDao.Properties.Synced.eq(false), DebtNoteDao.Properties.GlobalId.isNull).list()
        val debtNotes = ArrayList<SyncDebtNote>()
        for (n in list) {
            val debtNote = SyncDebtNote()
            debtNote.globalId = if (n.globalId == null) -1 else n.globalId
            debtNote.localId = n.id!!

            val debt = daoSession.debtDao.load(n.debtId)
            debtNote.globalDebtId = debt.globalId!!
            debtNote.text = n.text
            debtNote.date = n.date.time

            debtNotes.add(debtNote)
        }

        val trashList = daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(SyncConstants.DEBT_NOTE_TYPE)).list()
        for (trash in trashList) {
            val debtNote = SyncDebtNote()
            debtNote.removed = true
            debtNote.globalId = trash.globalId

            debtNotes.add(debtNote)
        }
        return debtNotes
    }

    override fun hasDirtyDebtNotes(): Boolean {
        val daoSession = dataManager.daoSession
        val updatedCount = daoSession.debtNoteDao.queryBuilder().whereOr(DebtNoteDao.Properties.Synced.eq(false), DebtNoteDao.Properties.GlobalId.isNull).count()
        return updatedCount > 0 || daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(SyncConstants.DEBT_NOTE_TYPE)).count() > 0
    }

    override fun createPattern(commandId: Long, p: SyncPattern) {
        val b = dataManager.patternService
                .newPatternBuilder()
                .putName(p.name)
                .putGlobalId(p.globalId)
                .putSynced(true)
                .putType(p.type)
                .putSourceAccountId(getAccountByGlobalId(p.globalSourceAccountId)!!.id)
                .putComment(p.comment)
                .putAmount(p.amount)

        val category = getCategoryByGlobalId(p.globalCategoryId)
        if (category != null) b.putCategoryId(category.id)

        val subcategory = getSubcategoryByGlobalId(p.globalSubcategoryId)
        if (subcategory != null) b.putSubcategoryId(subcategory.id)

        val account = getAccountByGlobalId(p.globalDestAccountId)
        if (account != null) b.putDestAccountId(account.id)

        if (p.destAmount != null) b.putDestAmount(p.destAmount)

        for (tag in p.tags) {
            b.putTag(tag)
        }
        b.createInternal()

        createSyncLog(SyncConstants.PATTERN_TYPE, commandId)
    }

    override fun updatePattern(commandId: Long, p: SyncPattern) {
        if (!isDeletedObject(SyncConstants.PATTERN_TYPE, p.globalId)) {
            val pattern = getPatternByGlobalId(p.globalId)
            if (pattern != null) {
                updatePattern(commandId, p, pattern)
            } else {
                // TODO info
            }
        } else {
            createSyncLog(SyncConstants.PATTERN_TYPE, commandId)
        }
    }

    private fun updatePattern(commandId: Long, p: SyncPattern, pattern: TransactionPattern) {
        val b = dataManager.patternService
                .newPatternBuilder()
                .putId(pattern.id)
                .putGlobalId(p.globalId)
                .putSynced(true)
                .putName(p.name)
                .putSourceAccountId(getAccountByGlobalId(p.globalSourceAccountId)!!.id)
                .putComment(p.comment)
                .putAmount(p.amount)

        val category = getCategoryByGlobalId(p.globalCategoryId)
        if (category != null) b.putCategoryId(category.id)

        val subcategory = getSubcategoryByGlobalId(p.globalSubcategoryId)
        if (subcategory != null) b.putSubcategoryId(subcategory.id)

        val account = getAccountByGlobalId(p.globalDestAccountId)
        if (account != null) b.putDestAccountId(account.id)

        if (p.destAmount != null) b.putDestAmount(p.destAmount)

        for (tag in p.tags) {
            b.putTag(tag)
        }
        b.updateInternal()

        createSyncLog(SyncConstants.PATTERN_TYPE, commandId)
    }

    private fun getPatternByGlobalId(globalId: Long): TransactionPattern? {
        val list = dataManager.daoSession.transactionPatternDao.queryBuilder()
                .where(TransactionPatternDao.Properties.GlobalId.eq(globalId)).list()
        return if (list.isEmpty()) null else list[0]
    }

    override fun deletePattern(commandId: Long, p: SyncPattern) {
        if (!isDeletedObject(SyncConstants.PATTERN_TYPE, p.globalId)) {
            val pattern = getPatternByGlobalId(p.globalId)
            if (pattern != null) dataManager.daoSession.delete(pattern)
        } else {
            clearTrash(SyncConstants.PATTERN_TYPE, p.globalId)
        }

        createSyncLog(SyncConstants.PATTERN_TYPE, commandId)
    }

    override fun mergePattern(commandId: Long, p: SyncPattern, sourceId: Long?) {
        val pattern = dataManager.daoSession.transactionPatternDao.load(sourceId)
        if (pattern != null) {
            updatePattern(commandId, p, pattern)
        } else {
            createPattern(commandId, p)
        }
    }

    override fun getDirtyPatterns(): List<SyncPattern> {
        val daoSession = dataManager.daoSession
        val list = daoSession.transactionPatternDao.queryBuilder().whereOr(TransactionPatternDao.Properties.Synced.eq(false), TransactionPatternDao.Properties.GlobalId.isNull).list()
        val patterns = ArrayList<SyncPattern>()
        for (p in list) {
            val pattern = SyncPattern()
            pattern.globalId = if (p.globalId == null) -1 else p.globalId
            pattern.localId = p.id!!
            pattern.name = p.name
            pattern.type = p.type
            pattern.globalSourceAccountId = p.sourceAccount.globalId!!
            pattern.comment = p.comment
            pattern.amount = p.amount

            pattern.globalCategoryId = if (p.categoryId != null) p.category.globalId else null
            pattern.globalSubcategoryId = if (p.subcategoryId != null) p.subcategory.globalId else null
            pattern.globalDestAccountId = if (p.destAccountId != null) p.destAccount.globalId else null
            pattern.destAmount = p.destAmount

            for (tag in p.tags) {
                pattern.tags.add(tag.tag.name)
            }


            patterns.add(pattern)
        }

        val trashList = daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(SyncConstants.PATTERN_TYPE)).list()
        for (trash in trashList) {
            val pattern = SyncPattern()
            pattern.removed = true
            pattern.globalId = trash.globalId

            patterns.add(pattern)
        }
        return patterns
    }

    override fun hasDirtyPatterns(): Boolean {
        val daoSession = dataManager.daoSession
        val updatedCount = daoSession.transactionPatternDao.queryBuilder().whereOr(TransactionPatternDao.Properties.Synced.eq(false), TransactionPatternDao.Properties.GlobalId.isNull).count()
        return updatedCount > 0 || daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(SyncConstants.PATTERN_TYPE)).count() > 0
    }

    override fun createBudget(commandId: Long, b: SyncBudget) {
        val builder = dataManager.budgetService.newBudgetBuilder()
                .putGlobalId(b.globalId)
                .putSynced(true)
                .putFrom(Date(b.start))
                .putTo(Date(b.finish))
                .putLimit(b.limit)
                .putType(b.type)
                .putName(b.name)
        if (b.nextGlobalId != null) {
            val next = getBudgetPlanByGlobalId(b.nextGlobalId)
            builder.putNextId(next?.id)
        }
        for (d in b.dependencies) {
            builder.putDependency(d.type, convertToLocalBudgetRefId(d.type, d.refGlobalId))
        }
        builder.createInternal()

        createSyncLog(SyncConstants.BUDGET_TYPE, commandId)
    }

    private fun getBudgetPlanByGlobalId(globalId: Long): BudgetPlan? {
        val list = dataManager.daoSession.budgetPlanDao.queryBuilder().where(BudgetPlanDao.Properties.GlobalId.eq(globalId)).list()
        return if (list.isEmpty()) null else list[0]
    }

    override fun updateBudget(commandId: Long, b: SyncBudget) {
        if (!isDeletedObject(SyncConstants.BUDGET_TYPE, b.globalId)) {
            val budgetPlan = getBudgetPlanByGlobalId(b.globalId)
            if (budgetPlan != null) {
                updateBudget(commandId, b, budgetPlan)
            } else {
                // TODO info
            }
        } else {
            createSyncLog(SyncConstants.BUDGET_TYPE, commandId)
        }
    }

    private fun updateBudget(commandId: Long, b: SyncBudget, budgetPlan: BudgetPlan) {
        val builder = dataManager.budgetService.newBudgetBuilder()
                .putId(budgetPlan.id)
                .putGlobalId(b.globalId)
                .putSynced(true)
                .putFrom(Date(b.start))
                .putTo(Date(b.finish))
                .putLimit(b.limit)
                .putType(b.type)
                .putName(b.name)

        val oldNextId = budgetPlan.next
        if (b.nextGlobalId == null) {
            builder.putNextId(oldNextId)
        } else {
            if (oldNextId != null) {
                dataManager.budgetService.deleteBudgetInternal(oldNextId)
            }

            val nextBudget = getBudgetPlanByGlobalId(b.nextGlobalId)
            builder.putNextId(nextBudget!!.id)
        }

        for (d in b.dependencies) {
            builder.putDependency(d.type, convertToLocalBudgetRefId(d.type, d.refGlobalId))
        }
        builder.updateInternal()
        createSyncLog(SyncConstants.BUDGET_TYPE, commandId)
    }

    override fun deleteBudget(commandId: Long, b: SyncBudget) {
        if (!isDeletedObject(SyncConstants.BUDGET_TYPE, b.globalId)) {
            val budgetPlan = getBudgetPlanByGlobalId(b.globalId)
            if (budgetPlan != null) dataManager.daoSession.delete(budgetPlan)
        } else {
            clearTrash(SyncConstants.BUDGET_TYPE, b.globalId)
        }

        createSyncLog(SyncConstants.BUDGET_TYPE, commandId)
    }

    override fun mergeBudget(commandId: Long, b: SyncBudget, sourceId: Long?) {
        val budget = dataManager.daoSession.budgetPlanDao.load(sourceId)
        if (budget != null) {
            updateBudget(commandId, b, budget)
        } else {
            createBudget(commandId, b)
        }
    }

    fun isDeletedObject(type: Int, globalId: Long): Boolean {
        return dataManager.daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(type), TrashDao.Properties.GlobalId.eq(globalId)).count() > 0
    }

    override fun getDirtyBudgets(changesList: ChangesList): List<SyncBudget> {
        val daoSession = dataManager.daoSession
        val list = daoSession.budgetPlanDao.queryBuilder().whereOr(BudgetPlanDao.Properties.GlobalId.isNull, BudgetPlanDao.Properties.Synced.eq(false)).list()
        prepareOrderCreation(list)

        val globalIds = ArrayMap<Long, Long>()
        val budgets = ArrayList<SyncBudget>()
        for (b in list) {
            val next = if (b.next == null) null else daoSession.budgetPlanDao.load(b.next)
            var nextGlobalId: Long?
            if (next != null && next.globalId == null) {
                nextGlobalId = globalIds[next.id]
                if (nextGlobalId == null) {
                    nextGlobalId = changesList.nextGlobalId()
                    globalIds.put(next.id, nextGlobalId)
                }
            } else {
                nextGlobalId = next?.globalId
            }

            val budget = SyncBudget()
            budget.globalId = if (b.globalId == null) -1 else b.globalId
            budget.localId = b.id!!
            budget.start = b.start.time
            budget.finish = b.finish.time
            budget.limit = b.limit
            budget.type = b.type
            budget.name = b.name
            budget.nextGlobalId = nextGlobalId

            for (d in b.dependencies) {
                budget.dependencies.add(convertToGlobalDependency(d))
            }
            budgets.add(budget)
        }

        val trashList = daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(SyncConstants.BUDGET_TYPE)).list()
        for (trash in trashList) {
            val budget = SyncBudget()
            budget.removed = true
            budget.globalId = trash.globalId

            budgets.add(budget)
        }

        budgets
                .filter { it.globalId < 0 }
                .forEach { it.preparedGlobalId = globalIds[it.localId] }

        return budgets
    }

    override fun hasDirtyBudgets(): Boolean {
        val daoSession = dataManager.daoSession
        val updatedCount = daoSession.budgetPlanDao.queryBuilder().whereOr(BudgetPlanDao.Properties.GlobalId.isNull, BudgetPlanDao.Properties.Synced.eq(false)).count()
        return updatedCount > 0 || daoSession.trashDao.queryBuilder().where(TrashDao.Properties.Type.eq(SyncConstants.BUDGET_TYPE)).count() > 0
    }

    override fun isOutdatedLock(lock: String, type: Int): Boolean {
        if (lock.isNullOrBlank()) return false

        val list = dataManager.daoSession.syncLockDao.queryBuilder().where(SyncLockDao.Properties.Type.eq(type), SyncLockDao.Properties.LockId.eq(lock)).limit(1).list()
        if (list.isEmpty()) return false

        val syncLock = list[0]
        return System.currentTimeMillis() - syncLock.time > SyncService.LOCK_LIVE
    }

    override fun saveLock(lock: String, type: Int) {
        val daoSession = dataManager.daoSession
        val list = daoSession.syncLockDao.queryBuilder().where(SyncLockDao.Properties.Type.eq(type)).limit(1).list()

        if (!list.isEmpty()) {
            val syncLock = list[0]
            syncLock.lockId = lock
            syncLock.time = System.currentTimeMillis()
            daoSession.update(syncLock)
        } else {
            val syncLock = SyncLock()
            syncLock.type = type
            syncLock.lockId = lock
            syncLock.time = System.currentTimeMillis()
            daoSession.insert(syncLock)
        }
    }

    private fun prepareOrderCreation(list: MutableList<BudgetPlan>) {
        val tempList = LinkedList(list)

        list.clear()
        val it = tempList.iterator()
        while (it.hasNext()) {
            val p = it.next()
            if (p.next == null) {
                list.add(p)
                it.remove()
            }
        }

        while (!tempList.isEmpty()) {
            val plan = tempList.removeAt(0)
            if (hasBudgetPlan(tempList, plan.next!!)) {
                // next is in temp and should be moved earlier than current
                tempList.add(plan)
            } else {
                list.add(plan)
            }
        }
    }

    private fun hasBudgetPlan(plans: List<BudgetPlan>, id: Long): Boolean {
        return plans.any { it.id == id }
    }

    private fun convertToLocalBudgetRefId(type: Int, globalRefId: String): String {
        when (type) {
            DataConstants.CATEGORY_TYPE -> {
                val category = getCategoryByGlobalId(java.lang.Long.parseLong(globalRefId))
                return category!!.id.toString()
            }
            DataConstants.SUBCATEGORY_TYPE -> {
                val subcategory = getSubcategoryByGlobalId(java.lang.Long.parseLong(globalRefId))
                return subcategory!!.id.toString()
            }
            DataConstants.TAG_TYPE -> {
                val daoSession = dataManager.daoSession
                val list = daoSession.tagDao.queryBuilder().where(TagDao.Properties.Name.eq(globalRefId)) // globalRefId is just tag name here
                        .limit(1).list()
                val tag: Tag
                if (list.isEmpty()) {
                    tag = Tag()
                    tag.name = globalRefId
                    tag.updated = Date()
                    daoSession.insert(tag)
                } else {
                    tag = list[0]
                }
                return tag.id.toString()
            }
            else -> throw RuntimeException("Unsupported type")
        }
    }

    private fun convertToGlobalDependency(d: BudgetPlanDependency): SyncBudget.Dependency {
        val dependency = SyncBudget.Dependency()
        dependency.type = d.refType
        val daoSession = dataManager.daoSession
        when (dependency.type) {
            DataConstants.CATEGORY_TYPE -> {
                val category = daoSession.categoryDao.load(java.lang.Long.parseLong(d.refId))
                dependency.refGlobalId = category.globalId.toString()
            }
            DataConstants.SUBCATEGORY_TYPE -> {
                val subcategory = daoSession.subcategoryDao.load(java.lang.Long.parseLong(d.refId))
                dependency.refGlobalId = subcategory.globalId.toString()
            }
            DataConstants.TAG_TYPE -> {
                val tag = daoSession.tagDao.load(java.lang.Long.parseLong(d.refId))
                dependency.refGlobalId = tag.name
            }
            else -> throw RuntimeException("Unsupported type")
        }
        return dependency
    }

    private fun clearDirty(item: ChangeItem) {
        val daoSession = dataManager.daoSession
        if (item.action == SyncConstants.CREATE_ACTION) {
            val syncObject = findSyncObject(item.objectWrapper.type, item.objectWrapper.obj.localId)
            syncObject.synced = true
            syncObject.globalId = item.objectWrapper.obj.globalId
            daoSession.update(syncObject)
        } else if (item.action == SyncConstants.UPDATE_ACTION) {
            val syncObject = findSyncObject(item.objectWrapper.type, item.objectWrapper.obj.localId)
            syncObject.synced = true
            daoSession.update(syncObject)
        } else if (item.action == SyncConstants.DELETE_ACTION) {
            clearTrash(item.objectWrapper.type, item.objectWrapper.obj.globalId)
        }
    }

    private fun clearTrash(type: Int, globalId: Long) {
        val daoSession = dataManager.daoSession
        val list = daoSession.trashDao.queryBuilder().where(TrashDao.Properties.GlobalId.eq(globalId), TrashDao.Properties.Type.eq(type)).list()
        for (t in list) {
            daoSession.delete(t)
        }
    }

    private fun findSyncObject(type: Int, localId: Long): ISyncObject {
        val daoSession = dataManager.daoSession
        if (type == SyncConstants.ACCOUNT_TYPE) {
            return daoSession.accountDao.load(localId)
        } else if (type == SyncConstants.CATEGORY_TYPE) {
            return daoSession.categoryDao.load(localId)
        } else if (type == SyncConstants.SUBCATEGORY_TYPE) {
            return daoSession.subcategoryDao.load(localId)
        } else if (type == SyncConstants.BUDGET_TYPE) {
            return daoSession.budgetPlanDao.load(localId)
        } else if (type == SyncConstants.DEBT_TYPE) {
            return daoSession.debtDao.load(localId)
        } else if (type == SyncConstants.DEBT_NOTE_TYPE) {
            return daoSession.debtNoteDao.load(localId)
        } else if (type == SyncConstants.TRANSACTION_TYPE) {
            return daoSession.transactionDao.load(localId)
        } else if (type == SyncConstants.PATTERN_TYPE) {
            return daoSession.transactionPatternDao.load(localId)
        } else {
            throw RuntimeException("Not supported yet.")
        }
    }
}
