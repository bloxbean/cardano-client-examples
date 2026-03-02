package com.bloxbean.cardano.client.example.tutorial.standards;

import com.bloxbean.cardano.client.cip.cip68.CIP68FT;
import com.bloxbean.cardano.client.cip.cip68.CIP68NFT;
import com.bloxbean.cardano.client.cip.cip68.CIP68RFT;
import com.bloxbean.cardano.client.cip.cip68.CIP68ReferenceToken;
import com.bloxbean.cardano.client.cip.cip68.common.CIP68File;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.transaction.spec.Asset;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

/**
 * CIP-68 Datum Metadata examples — creating datum-based metadata for tokens.
 *
 * <p>CIP-68 defines a metadata standard where token metadata is stored as inline datums
 * on reference UTXOs rather than in transaction metadata. This enables updatable metadata
 * and richer on-chain data structures.</p>
 *
 * <p>CIP-68 supports three token types:
 * <ul>
 *   <li>NFT (222) — Non-fungible tokens with reference tokens</li>
 *   <li>FT (333) — Fungible tokens with decimals and ticker</li>
 *   <li>RFT (444) — Rich fungible tokens with file attachments</li>
 * </ul>
 *
 * <p>Note: This example is standalone and does not require Yaci DevKit.
 * It focuses on datum construction — for minting with reference tokens, a Plutus script is required.</p>
 *
 * @see <a href="http://localhost:3000/docs/apis/standards/cip68-api">CIP-68 API Documentation</a>
 */
public class Cip68DatumMetadataExample {

    /**
     * Create a CIP-68 NFT datum with file attachments.
     *
     * <p>The NFT type (label 222) is for unique tokens. Each NFT has a reference token
     * (label 100) that holds the datum with the metadata.</p>
     */
    @Test
    public void createNftDatum() throws Exception {
        CIP68NFT nft = CIP68NFT.create()
                .name("MyCIP68NFT")
                .image("ipfs://QmExample123456789")
                .description("A sample CIP-68 NFT with datum metadata")
                .addFile(CIP68File.create()
                        .mediaType("image/png")
                        .name("image1.png")
                        .src("ipfs://QmHighRes456"));

        // Get the reference token (label 100) — always quantity 1
        CIP68ReferenceToken refToken = nft.getReferenceToken();
        Asset referenceAsset = refToken.getAsset();
        System.out.println("Reference token name: " + referenceAsset.getName());

        // Get the user token (label 222)
        Asset userToken = nft.getAsset(BigInteger.ONE);
        System.out.println("User token name: " + userToken.getName());

        // Get datum as PlutusData (for inline datum on the reference UTXO)
        ConstrPlutusData datum = nft.getDatumAsPlutusData();
        System.out.println("NFT datum: " + datum.serializeToHex());
    }

    /**
     * Create a CIP-68 FT (Fungible Token) datum.
     *
     * <p>The FT type (label 333) is for fungible tokens with metadata like
     * name, ticker, decimals, and logo.</p>
     */
    @Test
    public void createFtDatum() throws Exception {
        CIP68FT ft = CIP68FT.create()
                .name("SampleFungibleToken")
                .ticker("SFT")
                .url("https://example.com")
                .logo("ipfs://QmLogo123")
                .decimals(6)
                .description("A sample CIP-68 fungible token");

        // Reference token (label 100)
        CIP68ReferenceToken refToken = ft.getReferenceToken();
        Asset referenceAsset = refToken.getAsset();
        System.out.println("FT reference token: " + referenceAsset.getName());

        // User token (label 333)
        Asset userToken = ft.getAsset(BigInteger.valueOf(1_000_000));
        System.out.println("FT user token: " + userToken.getName());

        // Datum
        ConstrPlutusData datum = ft.getDatumAsPlutusData();
        System.out.println("FT datum: " + datum.serializeToHex());
    }

    /**
     * Create a CIP-68 RFT (Rich Fungible Token) datum.
     *
     * <p>The RFT type (label 444) is for fungible tokens that need richer metadata
     * like images, files, and custom properties — similar to NFTs but fungible.</p>
     */
    @Test
    public void createRftDatum() throws Exception {
        CIP68RFT rft = CIP68RFT.create()
                .name("SampleRichFungibleToken")
                .image("ipfs://QmRftImage789")
                .description("A sample CIP-68 rich fungible token")
                .decimals(2)
                .addFile(CIP68File.create()
                        .mediaType("image/png")
                        .name("logo.png")
                        .src("ipfs://QmRftLogo"))
                .property("category", "art");

        // Reference token (label 100)
        CIP68ReferenceToken refToken = rft.getReferenceToken();
        Asset referenceAsset = refToken.getAsset();
        System.out.println("RFT reference token: " + referenceAsset.getName());

        // User token (label 444)
        Asset userToken = rft.getAsset(BigInteger.valueOf(1_000));
        System.out.println("RFT user token: " + userToken.getName());

        // Datum
        ConstrPlutusData datum = rft.getDatumAsPlutusData();
        System.out.println("RFT datum: " + datum.serializeToHex());
    }

    public static void main(String[] args) throws Exception {
        Cip68DatumMetadataExample example = new Cip68DatumMetadataExample();

        System.out.println("=== CIP-68 Datum Metadata Examples ===\n");

        System.out.println("--- CIP-68 NFT Datum ---");
        example.createNftDatum();

        System.out.println("\n--- CIP-68 FT Datum ---");
        example.createFtDatum();

        System.out.println("\n--- CIP-68 RFT Datum ---");
        example.createRftDatum();
    }
}
