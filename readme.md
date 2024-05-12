# Hash Pre-Image

This project intends to create an original hash through the
class [SHA1Unit](src/main/java/com/github/valtoni/SHA1Unit.java)
and wants to compare it with the hash generated randomly by the
same class. The purpose of this project is use the project
loom (virtual threads) to improve the performance, a [CollisionTask](src/main/java/com/github/valtoni/CollisionTask.java)
that will call the first step and a [DynamicThreadAdjuster](src/main/java/com/github/valtoni/DynamicThreadAdjuster.java), 
that will adjust the number of threads according to the 
number of cores of the machine and its use.