package com.bloxbean.cardano.client.examples.function;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.cip.cip20.MessageMetadata;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.examples.BaseTest;
import com.bloxbean.cardano.client.exception.AddressExcepion;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilder;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.transaction.spec.Transaction;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.client.function.helper.AuxDataProviders.metadataProvider;
import static com.bloxbean.cardano.client.function.helper.ChangeOutputAdjustments.adjustChangeOutput;
import static com.bloxbean.cardano.client.function.helper.FeeCalculators.feeCalculator;
import static com.bloxbean.cardano.client.function.helper.InputBuilders.createFromSender;
import static com.bloxbean.cardano.client.function.helper.SignerProviders.signerFrom;

public class TransferTransactionMultiSenderMultiReceiverTest extends BaseTest {
    public static void main(String[] args) throws AddressExcepion, CborSerializationException, ApiException {
        new TransferTransactionMultiSenderMultiReceiverTest().transfer();
        System.exit(1);
    }

    public void transfer() throws CborSerializationException, ApiException, AddressExcepion {
        String senderMnemonic = "kit color frog trick speak employ suit sort bomb goddess jewel primary spoil fade person useless measure manage warfare reduce few scrub beyond era";
        Account sender = new Account(Networks.testnet(), senderMnemonic);
        String senderAddress = sender.baseAddress();

        String sender2Mnemonic = "essence pilot click armor alpha noise mixture soldier able advice multiply inject ticket pride airport uncover honey desert curtain sun true toast valve half";
        Account sender2 = new Account(Networks.testnet(), sender2Mnemonic);
        String sender2Address = sender2.baseAddress();

        String receiverAddress1 = "addr_test1qqwpl7h3g84mhr36wpetk904p7fchx2vst0z696lxk8ujsjyruqwmlsm344gfux3nsj6njyzj3ppvrqtt36cp9xyydzqzumz82";
        String receiverAddress2 = "addr_test1qqqvjp4ffcdqg3fmx0k8rwamnn06wp8e575zcv8d0m3tjn2mmexsnkxp7az774522ce4h3qs4tjp9rxjjm46qf339d9sk33rqn";
        String receiver3Address3 = "addr_test1qznuey056aczwykcs3t90yjm8up24rjda5vm7czs6uycueqasp723sny4y96tnzl8smzc7z3jmh0sxpu5hy59u50wp3q3mwvly";

        Output output1 = Output.builder()
                .address(receiverAddress1)
                .assetName(LOVELACE)
                .qty(adaToLovelace(2.1))
                .build();

        Output output2 = Output.builder()
                .address(receiverAddress2)
                .assetName(LOVELACE)
                .qty(adaToLovelace(2.1))
                .build();

        Output output3 = Output.builder()
                .address(receiver3Address3)
                .assetName(LOVELACE)
                .qty(adaToLovelace(2.1))
                .build();

        MessageMetadata metadata = MessageMetadata.create()
                .add("This is a sample transfer transaction")
                .add("with multiple receiver and senders");

        TxBuilder txBuilder = (output1.outputBuilder()
                .and(output2.outputBuilder())
                .and(output3.outputBuilder())
                .buildInputs(createFromSender(senderAddress, senderAddress))
        ).andThen(output1.outputBuilder()
                .and(output2.outputBuilder())
                .and(output3.outputBuilder())
                .buildInputs(createFromSender(sender2Address, sender2Address))
        )
                .andThen(metadataProvider(metadata))
                .andThen(feeCalculator(senderAddress, 2))
                .andThen(adjustChangeOutput(senderAddress, 2));

        Transaction signedTransaction = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier)
                .buildAndSign(txBuilder, signerFrom(sender, sender2));

        Result<String> result = transactionService.submitTransaction(signedTransaction.serialize());
        System.out.println(result);

        if (result.isSuccessful())
            System.out.println("Transaction Id: " + result.getValue());
        else
            System.out.println("Transaction failed: " + result);

        waitForTransactionHash(result);
    }
}
