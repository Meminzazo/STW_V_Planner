package com.meminzazo.stwvplanner.data.mapper

import com.meminzazo.stwvplanner.data.local.entity.AccountEntity
import com.meminzazo.stwvplanner.data.local.entity.TransactionEntity
import com.meminzazo.stwvplanner.domain.model.Account
import com.meminzazo.stwvplanner.domain.model.Transaction

fun AccountEntity.toDomain(balance: Int = 0): Account = Account(
    id = id,
    name = name,
    isMain = parentAccountId == null,
    balance = balance,
    parentAccountId = parentAccountId
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    parentAccountId = parentAccountId
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    accountId = accountId,
    amount = amount,
    type = type,
    source = source,
    description = description,
    date = date,
    recipientAccountName = recipientAccountName,
    senderAccountId = senderAccountId,
    receiverAccountId = receiverAccountId,
    itemType = itemType,
    itemName = itemName
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    accountId = accountId,
    amount = amount,
    type = type,
    source = source,
    description = description,
    date = date,
    recipientAccountName = recipientAccountName,
    senderAccountId = senderAccountId,
    receiverAccountId = receiverAccountId,
    itemType = itemType,
    itemName = itemName
)
