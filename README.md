#CSE 486/586 Distributed Systems
###Programming Assignment 2
###Group Messenger with a Local Persistent Key-Value Table with Total and FIFO Ordering Guarantees

####Introduction
This assignment builds on the previous simple messenger and has 2 parts. 

####Part 1
You will design a group messenger that can send message to multiple AVDs and store them in a permanent key-value storage.

####Part 2
In this part you will add ordering guarantees to your group messenger. The guarantees you will implement are total ordering as well as FIFO ordering. As with part A, you will store all the messages in your content provider. The difference is that when you store the messages and assign sequence numbers, your mechanism needs to provide total and FIFO ordering guarantees.
You will need to design an algorithm that does this and implement it. An important thing to keep in mind is that there will be a failure of an app instance in the middle of the execution.