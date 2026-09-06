package com.arhiplabs.unstuckly.data.repository

import com.arhiplabs.unstuckly.data.db.RuleDao
import com.arhiplabs.unstuckly.data.model.Rule
import kotlinx.coroutines.flow.Flow

class RuleRepository(private val ruleDao: RuleDao) {
    val activeRules: Flow<List<Rule>> = ruleDao.getActiveRules()
    val allRules: Flow<List<Rule>> = ruleDao.getAllRules()

    suspend fun getPrimaryActiveRule(): Rule? = ruleDao.getPrimaryActiveRule()

    suspend fun saveRule(rule: Rule): Long {
        return if (rule.id == 0L) {
            ruleDao.insertRule(rule)
        } else {
            ruleDao.updateRule(rule)
            rule.id
        }
    }

    suspend fun deleteRule(rule: Rule) {
        ruleDao.deleteRule(rule)
    }
}
