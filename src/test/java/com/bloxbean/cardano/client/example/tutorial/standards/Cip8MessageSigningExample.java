package com.bloxbean.cardano.client.example.tutorial.standards;

import co.nstant.in.cbor.model.ByteString;
import co.nstant.in.cbor.model.UnsignedInteger;
import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.cip.cip8.*;
import com.bloxbean.cardano.client.cip.cip8.builder.COSESign1Builder;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.config.Configuration;
import com.bloxbean.cardano.client.crypto.api.SigningProvider;
import com.bloxbean.cardano.client.util.HexUtil;
import org.junit.jupiter.api.Test;

/**
 * CIP-8 Message Signing examples — COSE Sign1 creation and verification.
 *
 * <p>CIP-8 defines a message signing standard for Cardano using COSE (CBOR Object
 * Signing and Encryption) structures. It enables off-chain message signing with
 * Cardano keys, useful for authentication, ownership proofs, and voting.</p>
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Creating a COSE Sign1 structure (single signer)</li>
 *   <li>Verifying a COSE Sign1 signature</li>
 *   <li>Working with COSE headers</li>
 * </ul>
 *
 * <p>Note: This example is standalone and does not require Yaci DevKit.</p>
 *
 * @see <a href="http://localhost:3000/docs/apis/standards/cip8-api">CIP-8 API Documentation</a>
 */
public class Cip8MessageSigningExample {

    private final SigningProvider signingProvider;

    public Cip8MessageSigningExample() {
        this.signingProvider = Configuration.INSTANCE.getSigningProvider();
    }

    /**
     * Create a COSE Sign1 structure — single signer message signing.
     *
     * <p>COSE Sign1 is the simplest signature structure: one signer, one signature.</p>
     */
    @Test
    public void createCoseSign1() throws Exception {
        Account account = new Account(Networks.testnet());
        byte[] payload = "Hello, Cardano!".getBytes();

        // Build protected headers
        HeaderMap protectedHeaderMap = new HeaderMap()
                .algorithmId(14L)   // EdDSA
                .contentType(0L);   // Content type

        Headers headers = new Headers()
                ._protected(new ProtectedHeaderMap(protectedHeaderMap))
                .unprotected(new HeaderMap());

        // Build COSE Sign1
        COSESign1Builder builder = new COSESign1Builder(headers, payload, false);

        // Create the signature structure to sign
        SigStructure sigStructure = builder.makeDataToSign();
        byte[] dataToSign = sigStructure.serializeAsBytes();

        // Sign with account's private key
        byte[] signature = signingProvider.signExtended(
                dataToSign,
                account.privateKeyBytes(),
                account.publicKeyBytes());

        // Build final COSE Sign1
        COSESign1 coseSign1 = builder.build(signature);

        byte[] serialized = coseSign1.serializeAsBytes();
        System.out.println("COSE Sign1 created successfully");
        System.out.println("Serialized size: " + serialized.length + " bytes");
        System.out.println("CBOR hex: " + HexUtil.encodeHexString(serialized));

        // Store for verification
        verifySignature(serialized, account.publicKeyBytes());
    }

    /**
     * Verify a COSE Sign1 signature.
     */
    private void verifySignature(byte[] coseSign1Bytes, byte[] publicKey) throws Exception {
        // Deserialize
        COSESign1 coseSign1 = COSESign1.deserialize(coseSign1Bytes);

        // Reconstruct the signed data
        SigStructure sigStructure = coseSign1.signedData();
        byte[] signedData = sigStructure.serializeAsBytes();

        // Get the signature
        byte[] signature = coseSign1.signature();

        // Verify
        boolean valid = signingProvider.verify(signature, signedData, publicKey);
        System.out.println("\nSignature verification: " + (valid ? "VALID" : "INVALID"));
    }

    /**
     * Working with COSE headers — extracting header values.
     */
    @Test
    public void coseHeaders() throws Exception {
        // Create headers with various fields
        HeaderMap protectedHeaders = new HeaderMap()
                .algorithmId(14L)
                .contentType(0L);

        HeaderMap unprotectedHeaders = new HeaderMap()
                .addOtherHeader(99L, new ByteString("custom-value".getBytes()));

        Headers headers = new Headers()
                ._protected(new ProtectedHeaderMap(protectedHeaders))
                .unprotected(unprotectedHeaders);

        System.out.println("Protected headers created");
        System.out.println("Unprotected headers created");

        // Build a COSE Sign1 with these headers
        byte[] payload = "Header test".getBytes();
        COSESign1Builder builder = new COSESign1Builder(headers, payload, false);
        SigStructure sigStructure = builder.makeDataToSign();

        System.out.println("SigStructure context: " + sigStructure.sigContext());
        System.out.println("SigStructure serialized size: " + sigStructure.serializeAsBytes().length + " bytes");
    }

    /**
     * Create a COSE Key structure.
     */
    @Test
    public void coseKeys() {
        Account account = new Account(Networks.testnet());

        COSEKey coseKey = new COSEKey()
                .keyType(1L)           // OKP
                .algorithmId(14L)      // EdDSA
                .addOtherHeader(-1L, new UnsignedInteger(6))  // crv: Ed25519
                .addOtherHeader(-2L, new ByteString(account.publicKeyBytes())); // x: public key

        byte[] serialized = coseKey.serializeAsBytes();
        System.out.println("COSE Key created");
        System.out.println("COSE Key hex: " + HexUtil.encodeHexString(serialized));
        System.out.println("COSE Key size: " + serialized.length + " bytes");
    }

    public static void main(String[] args) throws Exception {
        Cip8MessageSigningExample example = new Cip8MessageSigningExample();

        System.out.println("=== CIP-8 Message Signing Examples ===\n");

        System.out.println("--- COSE Sign1 (Create & Verify) ---");
        example.createCoseSign1();

        System.out.println("\n--- COSE Headers ---");
        example.coseHeaders();

        System.out.println("\n--- COSE Keys ---");
        example.coseKeys();
    }
}
