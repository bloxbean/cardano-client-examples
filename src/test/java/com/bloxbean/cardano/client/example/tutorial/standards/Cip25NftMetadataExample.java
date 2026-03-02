package com.bloxbean.cardano.client.example.tutorial.standards;

import com.bloxbean.cardano.client.cip.cip25.NFT;
import com.bloxbean.cardano.client.cip.cip25.NFTFile;
import com.bloxbean.cardano.client.cip.cip25.NFTMetadata;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadata;
import com.bloxbean.cardano.client.util.HexUtil;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

/**
 * CIP-25 NFT Metadata examples — creating and structuring NFT metadata.
 *
 * <p>CIP-25 defines the NFT metadata standard for Cardano. The metadata is stored
 * in the transaction metadata under label 721 and includes fields like name, image,
 * description, media type, and custom properties.</p>
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Creating basic NFT metadata entries</li>
 *   <li>Adding file references to NFTs</li>
 *   <li>Custom attributes/properties</li>
 *   <li>Merging NFT metadata with other metadata</li>
 * </ul>
 *
 * <p>Note: This example is standalone and does not require Yaci DevKit.
 * It focuses on metadata construction only — for minting, see the QuickTx API tutorial.</p>
 *
 * @see <a href="http://localhost:3000/docs/apis/standards/cip25-api">CIP-25 API Documentation</a>
 */
public class Cip25NftMetadataExample {

    // Example policy ID (would come from a native or Plutus script in production)
    private static final String POLICY_ID = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef12";

    /**
     * Create basic NFT metadata for a single token.
     */
    @Test
    public void createNftMetadata() throws Exception {
        NFT nft = NFT.create()
                .assetName("MyAwesomeNFT")
                .name("My Awesome NFT")
                .image("ipfs://QmExample123456789")
                .mediaType("image/png")
                .description("A sample CIP-25 NFT created with CCL");

        // Wrap in NFTMetadata (label 721)
        NFTMetadata nftMetadata = NFTMetadata.create()
                .addNFT(POLICY_ID, nft);

        byte[] serialized = nftMetadata.serialize();
        System.out.println("NFT metadata CBOR: " + HexUtil.encodeHexString(serialized));
        System.out.println("NFT metadata size: " + serialized.length + " bytes");
    }

    /**
     * Create an NFT with multiple file references.
     */
    @Test
    public void nftWithFiles() throws Exception {
        NFTFile thumbnailFile = NFTFile.create()
                .mediaType("image/png")
                .name("thumbnail.png")
                .src("ipfs://QmThumbnail123");

        NFTFile highResFile = NFTFile.create()
                .mediaType("image/png")
                .name("highres.png")
                .src("ipfs://QmHighRes456");

        NFTFile animationFile = NFTFile.create()
                .mediaType("video/mp4")
                .name("animation.mp4")
                .src("ipfs://QmAnimation789");

        NFT nft = NFT.create()
                .assetName("MultiFileNFT")
                .name("Multi-File NFT")
                .image("ipfs://QmThumbnail123")
                .description("An NFT with multiple file attachments")
                .addFile(thumbnailFile)
                .addFile(highResFile)
                .addFile(animationFile);

        NFTMetadata metadata = NFTMetadata.create()
                .addNFT(POLICY_ID, nft);

        byte[] serialized = metadata.serialize();
        System.out.println("Multi-file NFT metadata: " + HexUtil.encodeHexString(serialized));
    }

    /**
     * Add custom attributes/properties to an NFT.
     */
    @Test
    public void customAttributes() throws Exception {
        NFT nft = NFT.create()
                .assetName("GameCharacter001")
                .name("Warrior #001")
                .image("ipfs://QmCharacter001")
                .description("A game character NFT with custom attributes")
                .property("power", "100")
                .property("defense", "85")
                .property("rarity", "legendary")
                .property("element", "fire");

        NFTMetadata metadata = NFTMetadata.create()
                .addNFT(POLICY_ID, nft);

        byte[] serialized = metadata.serialize();
        System.out.println("NFT with custom attributes: " + HexUtil.encodeHexString(serialized));
    }

    /**
     * Merge NFT metadata with other custom metadata.
     */
    @Test
    public void mergeMetadata() throws Exception {
        // Create NFT metadata (label 721)
        NFT nft = NFT.create()
                .assetName("MergedNFT")
                .name("Merged Metadata NFT")
                .image("ipfs://QmMerged123");

        NFTMetadata nftMetadata = NFTMetadata.create()
                .addNFT(POLICY_ID, nft);

        // Create custom metadata at a different label
        CBORMetadata customMetadata = new CBORMetadata();
        customMetadata.put(BigInteger.valueOf(100), "Custom application data");
        customMetadata.put(BigInteger.valueOf(200), "Additional info");

        // Merge: custom metadata + NFT metadata
        customMetadata.merge(nftMetadata);

        byte[] serialized = customMetadata.serialize();
        System.out.println("Merged metadata (custom + NFT): " + HexUtil.encodeHexString(serialized));
        System.out.println("Merged metadata size: " + serialized.length + " bytes");
    }

    public static void main(String[] args) throws Exception {
        Cip25NftMetadataExample example = new Cip25NftMetadataExample();

        System.out.println("=== CIP-25 NFT Metadata Examples ===\n");

        System.out.println("--- Create NFT Metadata ---");
        example.createNftMetadata();

        System.out.println("\n--- NFT with Files ---");
        example.nftWithFiles();

        System.out.println("\n--- Custom Attributes ---");
        example.customAttributes();

        System.out.println("\n--- Merge Metadata ---");
        example.mergeMetadata();
    }
}
