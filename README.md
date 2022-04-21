# Event-sourcing ATM

Simple implementation of ATM and notifier application.

ATM application allows creating account (without created account balance operations are not allowed)
and balance operations: Debit, Withdrawal. With Withdrawal operation balance cannot fall below zero.

Notifier application sends notifications when account balance reach certain conditions (falls below 100 Eur).

The same key (accountId) is used which guarantees that events will land on the same partitions maintaining order.

#### Dockerization

Application can be dockerized using following command:

```sbt docker:publishLocal```

And started with:

```
docker run -e BOOTSTRAP_SERVER="<local IP>:9092" --rm atm-application:1.0
docker run -e BOOTSTRAP_SERVER="<local IP>:9092" --rm notifier-application:1.0
```
List of required topics:

```
atm-account-operations-topic  -> account operations
atm-balance-operations-topic  -> balance operations
atm-operations-result-topic   -> results of account and balance operations
balance-change-event-topic    -> source of truth about balance changes 
balance-notification-topic    -> notifications about balance changes
```