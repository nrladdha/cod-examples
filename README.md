# Apache Phoenix Thick Client - Customer Table with JSON

This project demonstrates how to use Apache Phoenix thick client to read and write records to Phoenix tables with JSON data storage using JDBC connections.

## Overview


## Project Structure


## Prerequisites

1. **Java Development Kit (JDK) 8 or higher**
   ```bash
   java -version
   ```

2. **Apache Maven**
   ```bash
   mvn -version
   ```

3. **Apache HBase & Phoenix**
   - HBase 2.4.x or compatible version
   - Phoenix 5.1.x or compatible version
   - Both services should be running and accessible

4. **ZooKeeper**
   - Should be running as part of HBase cluster

## Configuration


### 2. Update Connection URL (Optional)

**JDBC URL Format:**
```
jdbc:phoenix:zookeeper_quorum:port:znode_parent
```

## Building the Project

### Using Maven

```bash
# Clean and compile
mvn clean compile

# Create executable JAR with dependencies
mvn clean package

# Skip tests (if any)
mvn clean package -DskipTests
```

The compiled JAR will be located at: `target/`

## Running the Application


## Key Features

### Phoenix Thick Client

### UPSERT Operation

### Auto-commit

### JSON Data Handling

### Batch Operations

## Connection Properties

## Sample Output

## Troubleshooting

### Connection Issues

### ClassNotFoundException

### Version Compatibility

## Additional Resources

- [Apache Phoenix Documentation](https://phoenix.apache.org/)
- [Phoenix JDBC API](https://phoenix.apache.org/server.html)
- [HBase Documentation](https://hbase.apache.org/)
- [Phoenix Grammar](https://phoenix.apache.org/language/index.html)

