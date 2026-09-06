package com.arhiplabs.unstuckly.fakes

import com.arhiplabs.unstuckly.data.db.RuleDao
import com.arhiplabs.unstuckly.data.model.Rule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeRuleDao(initialRules: List<Rule> = emptyList()) : RuleDao {

    private val rulesMap = LinkedHashMap<Long, Rule>()
    private var nextId = 1L
    private val _rulesFlow = MutableStateFlow<List<Rule>>(emptyList())

    init {
        for (r in initialRules) {
            val id = if (r.id == 0L) nextId++ else r.id
            rulesMap[id] = r.copy(id = id)
            if (id >= nextId) nextId = id + 1
        }
        _rulesFlow.value = rulesMap.values.toList()
    }

    override fun getActiveRules(): Flow<List<Rule>> {
        return _rulesFlow.map { list -> list.filter { it.isActive } }
    }

    override suspend fun getPrimaryActiveRule(): Rule? {
        return rulesMap.values.firstOrNull { it.isActive }
    }

    override fun getAllRules(): Flow<List<Rule>> {
        return _rulesFlow.asStateFlow()
    }

    override suspend fun insertRule(rule: Rule): Long {
        val id = if (rule.id == 0L) nextId++ else rule.id
        val newRule = rule.copy(id = id)
        rulesMap[id] = newRule
        if (id >= nextId) nextId = id + 1
        _rulesFlow.value = rulesMap.values.toList()
        return id
    }

    override suspend fun updateRule(rule: Rule) {
        rulesMap[rule.id] = rule
        _rulesFlow.value = rulesMap.values.toList()
    }

    override suspend fun deleteRule(rule: Rule) {
        rulesMap.remove(rule.id)
        _rulesFlow.value = rulesMap.values.toList()
    }
}
