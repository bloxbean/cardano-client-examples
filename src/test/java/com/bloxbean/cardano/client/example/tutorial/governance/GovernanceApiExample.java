package com.bloxbean.cardano.client.example.tutorial.governance;

import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.example.tutorial.TutorialBase;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.transaction.spec.governance.*;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.GovActionId;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.InfoAction;
import com.bloxbean.cardano.client.util.HexUtil;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/**
 * Governance API examples — DRep registration, voting, and proposals.
 *
 * <p>The Governance API supports Cardano's on-chain governance (Conway era).
 * It provides transaction operations for DRep registration, vote delegation,
 * governance proposals, and voting.</p>
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Registering as a DRep (Delegated Representative)</li>
 *   <li>Delegating voting power to a DRep</li>
 *   <li>Creating an Info governance proposal</li>
 *   <li>Casting a vote on a proposal</li>
 * </ul>
 *
 * <p>Note: Methods must be run in order (register DRep, then delegate, then propose, then vote).
 * Governance operations require Conway era to be active on the network.</p>
 *
 * @see <a href="http://localhost:3000/docs/apis/governance/governance-api">Governance API Documentation</a>
 */
public class GovernanceApiExample extends TutorialBase {

    private String proposalTxHash;

    /**
     * Register account1 as a DRep.
     *
     * <p>DRep registration requires a deposit (defined by protocol parameters)
     * and an anchor (metadata URL + hash).</p>
     */
    @Test
    @Order(1)
    public void registerDRep() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        // Create an anchor (metadata URL and data hash)
        Anchor anchor = new Anchor(
                "https://example.com/drep-metadata.json",
                HexUtil.decodeHexString("0000000000000000000000000000000000000000000000000000000000000000")
        );

        Tx tx = new Tx()
                .registerDRep(account1, anchor)
                .from(address1);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(account1))
                .withSigner(SignerProviders.drepKeySignerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("DRep registration result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getValue());
            System.out.println("DRep ID: " + account1.drepId());
        } else {
            System.err.println("Failed: " + result.getResponse());
        }
    }

    /**
     * Delegate voting power from account2 to account1's DRep.
     */
    @Test
    @Order(2)
    public void voteDelegation() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        // Create DRep reference from account1's DRep credential
        DRep drep = DRep.addrKeyHash(account1.drepCredential().getBytes());

        Tx tx = new Tx()
                .delegateVotingPowerTo(address2, drep)
                .from(address2);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(account2))
                .withSigner(SignerProviders.stakeKeySignerFrom(account2))
                .completeAndWait(System.out::println);

        System.out.println("Vote delegation result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getValue());
        } else {
            System.err.println("Failed: " + result.getResponse());
        }
    }

    /**
     * Create an Info governance proposal.
     *
     * <p>Info actions are the simplest governance proposal type — they don't
     * change any protocol state, just record information on-chain.</p>
     */
    @Test
    @Order(3)
    public void createInfoProposal() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        InfoAction infoAction = new InfoAction();

        Anchor anchor = new Anchor(
                "https://example.com/proposal-metadata.json",
                HexUtil.decodeHexString("0000000000000000000000000000000000000000000000000000000000000000")
        );

        Tx tx = new Tx()
                .createProposal(infoAction, account1.stakeAddress(), anchor)
                .from(address1);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("Info proposal result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            proposalTxHash = result.getValue();
            System.out.println("Proposal tx hash: " + proposalTxHash);
        } else {
            System.err.println("Failed: " + result.getResponse());
        }
    }

    /**
     * Cast a vote on the info proposal.
     *
     * <p>Votes as account1's DRep on the previously created proposal.</p>
     */
    @Test
    @Order(4)
    public void castVote() {
        if (proposalTxHash == null) {
            System.err.println("No proposal to vote on. Run createInfoProposal() first.");
            return;
        }

        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        // Create voter (DRep voter)
        Voter voter = new Voter(VoterType.DREP_KEY_HASH, account1.drepCredential());

        // Reference the governance action (proposal tx hash, index 0)
        GovActionId govActionId = new GovActionId(proposalTxHash, 0);

        Tx tx = new Tx()
                .createVote(voter, govActionId, Vote.YES)
                .from(address1);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(account1))
                .withSigner(SignerProviders.drepKeySignerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("Vote result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Vote tx hash: " + result.getValue());
        } else {
            System.err.println("Failed: " + result.getResponse());
        }
    }

    public static void main(String[] args) {
        GovernanceApiExample example = new GovernanceApiExample();

        System.out.println("=== Governance API Examples ===\n");

        System.out.println("--- Register DRep ---");
        example.registerDRep();

        System.out.println("\n--- Vote Delegation ---");
        example.voteDelegation();

        System.out.println("\n--- Create Info Proposal ---");
        example.createInfoProposal();

        System.out.println("\n--- Cast Vote ---");
        example.castVote();
    }
}
