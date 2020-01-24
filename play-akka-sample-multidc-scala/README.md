## How to run


1. In terminal 1: `sbt "runMain sample.cassandra.DbLauncher"`

2. In terminal 2: `sbt "runMain sample.cluster.ClusterDriver 2551 eu-west"`

3. In terminal 3: `sbt "runMain sample.cluster.ClusterDriver 2552 eu-central"`

4. In terminal 4: `sbt run` 
  * This will start the Play API
  
In another terminal (or with another tool such as Postman), resources can be interacted with via posts to the endpoints defined in the `routes` file. Resources only need either a correlation id (`id`) and or an `amount` sent as a JSON POST.

  