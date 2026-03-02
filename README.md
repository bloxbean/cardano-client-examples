# cardano-client-examples

Examples for [cardano-client-lib](https://github.com/bloxbean/cardano-client-lib)

## 📚 Documentation Site

**[View Full Documentation →](https://cardano-client.dev)**

Comprehensive examples with detailed explanations for:
- Simple payments and transfers
- Token minting and NFTs
- Plutus smart contracts
- Staking and delegation
- Advanced features (time locks, CIP-8, key derivation)

## Quick Start

Examples are located under `src/test/java` source root.

### Prerequisites

- Cardano Client Lib 0.8.0-preview1 or higher
- Java 17 or higher
- Maven
- Blockfrost API key (get one at [blockfrost.io](https://blockfrost.io))

### Configuration

Add your Blockfrost testnet project ID in `Constant.java`:

```java
public static final String BLOCKFROST_PROJECT_ID = "your-project-id";
```

### Running Examples

```bash
# Run a specific example
mvn test -Dtest=SimplePayment

# Run all QuickTx examples
mvn test -Dtest="com.bloxbean.cardano.client.example.quicktx.*"
```

## Example Categories (under `src/test/`)

- **tutorial/** - Example code from cardano-client.dev docs
- **quicktx/** - High-level QuickTx API examples (recommended for beginners)
- **function/** - Function-based composition examples
- **timelock/** - Time-based transaction constraints
- **cip8/** - Message signing (CIP-8 standard)
- **derivation/** - HD wallet key derivation
