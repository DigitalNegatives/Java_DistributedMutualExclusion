 A Tree-Based Simulation Algorithm of Distributed Mutual Exclusion
===================

Overview
------
An implementation of Kerry Raymond’s paper “A Tree-Based Algorithm for Distributed Mutual Exclusion” ACM Vol. 7, No. 1, February 1989, Pages 61-77

The paper describes an algorithm for distributed mutual exclusion in a computer network of N nodes that communicate by messages rather than shared memory. The algorithm uses a spanning tree of the computer network, and the number of messages exchanged per critical section depends on the topology of this tree. However, typically the number of messages exchanged is O(log N) under light demand, and reduces to approximately four messages under saturated demand.

Each node holds information only about its immediate neighbors in the spanning tree rather than information about all nodes, and failed nodes can recover necessary information from their neighbors. The algorithm does not require sequence numbers as it operates correctly despite message overtaking.

Code Implementation
--------
The Mediator pattern is used. There is one mediator and a number of nodes. All nodes are threads as well as the mediator. All messages that a node wishes to send to another node is sent to the mediator's message queue. The mediator will remove a message from the queue and service the message.

#### <i class="icon-file"></i> Code Info:
- File Name: simRaymond.java
  
- Input: User interface available to choose load and node count

- Output: The screen will print the messages that are being sent. At the end of the simulation the number of messages per critical section will be displayed 

#### <i class="icon-bug"></i> Issues:
On certain Java environments the application will hang during the simulation. This issue is being looked into.

#### <i class="icon-file"></i> Reference: 
Kerry Raymond's paper "A Tree-Based Algorithm for Distributed Mutual Exclusion" ACM Vol. 7, No. 1, February 1989, Pages 61-77