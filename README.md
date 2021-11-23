# Prime Number Service (with Akka)

## Implementation choices
1. For REST and gRPC services I selected Akka stack as it's a test task from Dixa and, to my knowledge, Akka is your preferred choice.
2. For calculating prime numbers, I selected Sieve of Eratosthenes algorithm as it's the simplest yet still efficient. Besides, the purpose of task is not about implementation of algorithms, to my understanding.
3. I used Lightbend Akka-gRPC template to create the project, which may be visible in some places
4. The file structure is flat as there are only 5 Scala files and adding additional packages makes no sense to me

## How to run
To compile and start a server, run

`sbt "runMain com.example.primenumber.PrimeNumberServer"`

It launches the server on port 8080. If needed, you can specify the host and port at `application.conf` file, `server` section.

To start a proxy service, run 

`sbt "runMain com.example.primenumber.ProxyService"`

It launches a service on port 8081. If needed, you can specify the host and port at `application.conf` file, `proxyService` section.

After the launch, you can access the service at `http://127.0.0.1:8081/prime/17`
