package com.bloxbean.cardano.client.examples.function.contract;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.AddressService;
import com.bloxbean.cardano.client.backend.model.Result;
import com.bloxbean.cardano.client.cip.cip20.MessageMetadata;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilder;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.function.TxSigner;
import com.bloxbean.cardano.client.function.helper.*;
import com.bloxbean.cardano.client.transaction.spec.*;
import com.bloxbean.cardano.client.util.Tuple;

import java.math.BigInteger;
import java.util.Arrays;

//Minthttps://github.com/input-output-hk/cardano-node/blob/28c34d813b8176afc653d6612d59fdd37dfeecfb/plutus-example/src/Cardano/PlutusExample/MintingScript.hs#L1
public class MintContractAndDistributeTest extends ContractBaseTest {
    String senderMnemonic = "kit color frog trick speak employ suit sort bomb goddess jewel primary spoil fade person useless measure manage warfare reduce few scrub beyond era";
    Account sender = new Account(Networks.testnet(), senderMnemonic);
    String senderAddress = sender.baseAddress();

    public static void main(String[] args) throws Exception {
        new MintContractAndDistributeTest().mint();
    }

    void mint() throws Exception {
        String receiver1 = "addr_test1qrs2a2hjfs2wt8r3smzwmptezmave3yjgws068hp0qsflmcypglx0rl69tp49396282ns02caz4cx7a2n290h2df0j3qjku4dy";
        String receiver2 = "addr_test1qz9q4mps4skpu7wc63vk6yd4jj2qkheydkuznwgkfg89v397hm954glfvxv4hzcjvladwfh4c0l39uh5pkkcuj789zfs8awfnq";
        String receiver3 = "addr_test1qrth48wp98vampmpw602ryh29e0snhzzasmkt4aqfduk6hu0wcpcxeexmya86slcnh8ug6dl8njh2vn8v4tj2z0lv75swkcw3g";
        String receiver4 = "addr_test1qpy34jdakhx5nuenlv8v2ydwlkpa6e24v5l6ka2xnq4rtj6zx7td26wen4smhe8vjne4r4k3tm2g03radqv3g8ez6jyqzhqvh5";

        PlutusScript mintScript = PlutusScript.builder()
                .type("PlutusScriptV1")
                .cborHex("59083159082e010000323322332232323233322232333222323333333322222222323332223233332222323233223233322232333222323233223322323233333222223322332233223322332233222232325335302f332203330430043333573466e1cd55cea8012400046600e64646464646464646464646666ae68cdc39aab9d500a480008cccccccccc054cd408c8c8c8cccd5cd19b8735573aa004900011980d981b1aba150023028357426ae8940088d4158d4c15ccd5ce249035054310005849926135573ca00226ea8004d5d0a80519a8118121aba150093335502a75ca0526ae854020ccd540a9d728149aba1500733502303f35742a00c66a04666aa0a2090eb4d5d0a8029919191999ab9a3370e6aae7540092000233501d3232323333573466e1cd55cea80124000466a04a66a07ceb4d5d0a80118219aba135744a00446a0b46a60b666ae712401035054310005c49926135573ca00226ea8004d5d0a8011919191999ab9a3370e6aae7540092000233502333503e75a6ae854008c10cd5d09aba2500223505a35305b3357389201035054310005c49926135573ca00226ea8004d5d09aba250022350563530573357389201035054310005849926135573ca00226ea8004d5d0a80219a811bae35742a00666a04666aa0a2eb8140d5d0a801181a9aba135744a00446a0a46a60a666ae712401035054310005449926135744a00226ae8940044d5d1280089aba25001135744a00226ae8940044d5d1280089aba25001135573ca00226ea8004d5d0a8011919191999ab9a3370ea00290031180d181b9aba135573ca00646666ae68cdc3a801240084603260826ae84d55cf280211999ab9a3370ea00690011180c98161aba135573ca00a46666ae68cdc3a80224000460386eb8d5d09aab9e500623504d35304e3357389201035054310004f49926499264984d55cea80089baa001357426ae8940088d4118d4c11ccd5ce2490350543100048499261047135045353046335738920103505435000474984d55cf280089baa0012212330010030022001222222222212333333333300100b00a00900800700600500400300220012212330010030022001122123300100300212001122123300100300212001122123300100300212001212222300400521222230030052122223002005212222300100520011232230023758002640026aa06a446666aae7c004940388cd4034c010d5d080118019aba200203423232323333573466e1cd55cea801a4000466600e6464646666ae68cdc39aab9d5002480008cc034c0c4d5d0a80119a8098169aba135744a00446a06e6a607066ae712401035054310003949926135573ca00226ea8004d5d0a801999aa805bae500a35742a00466a01eeb8d5d09aba25002235033353034335738921035054310003549926135744a00226aae7940044dd50009110919980080200180110009109198008018011000899aa800bae75a224464460046eac004c8004d540bc88c8cccd55cf80112804919a80419aa81918031aab9d5002300535573ca00460086ae8800c0bc4d5d08008891001091091198008020018900089119191999ab9a3370ea002900011a80418029aba135573ca00646666ae68cdc3a801240044a01046a0546a605666ae712401035054310002c499264984d55cea80089baa001121223002003112200112001232323333573466e1cd55cea8012400046600c600e6ae854008dd69aba135744a00446a0486a604a66ae71241035054310002649926135573ca00226ea80048848cc00400c00880048c8cccd5cd19b8735573aa002900011bae357426aae7940088d4080d4c084cd5ce24810350543100022499261375400224464646666ae68cdc3a800a40084a00e46666ae68cdc3a8012400446a014600c6ae84d55cf280211999ab9a3370ea00690001280511a8119a981219ab9c490103505431000254992649926135573aa00226ea8004484888c00c0104488800844888004480048c8cccd5cd19b8750014800880188cccd5cd19b8750024800080188d406cd4c070cd5ce249035054310001d499264984d55ce9baa0011220021220012001232323232323333573466e1d4005200c200b23333573466e1d4009200a200d23333573466e1d400d200823300b375c6ae854014dd69aba135744a00a46666ae68cdc3a8022400c46601a6eb8d5d0a8039bae357426ae89401c8cccd5cd19b875005480108cc048c050d5d0a8049bae357426ae8940248cccd5cd19b875006480088c050c054d5d09aab9e500b23333573466e1d401d2000230133016357426aae7940308d4080d4c084cd5ce2481035054310002249926499264992649926135573aa00826aae79400c4d55cf280109aab9e500113754002424444444600e01044244444446600c012010424444444600a010244444440082444444400644244444446600401201044244444446600201201040024646464646666ae68cdc3a800a400446660106eb4d5d0a8021bad35742a0066eb4d5d09aba2500323333573466e1d400920002300a300b357426aae7940188d4044d4c048cd5ce2490350543100013499264984d55cea80189aba25001135573ca00226ea80048488c00800c888488ccc00401401000c80048c8c8cccd5cd19b875001480088c018dd71aba135573ca00646666ae68cdc3a80124000460106eb8d5d09aab9e500423500b35300c3357389201035054310000d499264984d55cea80089baa0012122300200321223001003200120011122232323333573466e1cd55cea80124000466aa016600c6ae854008c014d5d09aba25002235007353008335738921035054310000949926135573ca00226ea8004498480048004448848cc00400c008448004448c8c00400488cc00cc0080080041")
                .build();

        //-- Custom guess contract setup
        String mintScriptAddress = AddressService.getInstance().getEntAddress(mintScript, Networks.testnet()).getAddress();
        System.out.println("Mint Script Address: " + mintScriptAddress);

        //-- Setup collateral
        String collateral = "ab44e0f5faf56154cc33e757c9d98a60666346179d5a7a0b9d77734c23c42082";
        int collateralIndex = 0;

        Tuple<String, Integer> collateralTuple = checkCollateral(sender, collateral, collateralIndex);
        if (collateralTuple == null) {
            System.out.println("Collateral cannot be found or created. " + collateral);
            return;
        }
        collateral = collateralTuple._1;
        collateralIndex = collateralTuple._2;

        PlutusData redeemerData = new BigIntPlutusData(BigInteger.valueOf(4444)); //any redeemer .. doesn't matter

        long totalToken = 4000;
        String tokenName = "STT";
        MultiAsset multiAsset = MultiAsset.builder()
                .policyId(mintScript.getPolicyId())
                .assets(Arrays.asList(
                        Asset.builder()
                                .name("STT")
                                .value(BigInteger.valueOf(totalToken))
                                .build()))
                .build();

        Output output1 = Output.builder()
                .address(receiver1)
                .policyId(mintScript.getPolicyId())
                .assetName(tokenName)
                .qty(BigInteger.valueOf(1000)).build();

        Output output2 = Output.builder()
                .address(receiver2)
                .policyId(mintScript.getPolicyId())
                .assetName(tokenName)
                .qty(BigInteger.valueOf(1000)).build();

        Output output3 = Output.builder()
                .address(receiver3)
                .policyId(mintScript.getPolicyId())
                .assetName(tokenName)
                .qty(BigInteger.valueOf(500)).build();

        Output output4 = Output.builder()
                .address(receiver4)
                .policyId(mintScript.getPolicyId())
                .assetName(tokenName)
                .qty(BigInteger.valueOf(1500)).build();

        MessageMetadata metadata = MessageMetadata.create()
                .add("NFT minted by Plutus script");

        ExUnits exUnits = ExUnits.builder()
                .mem(BigInteger.valueOf(2289624))
                .steps(BigInteger.valueOf(1214842019)).build();

        TxBuilder txBuilder =
                output1.mintOutputBuilder()
                        .and(output2.mintOutputBuilder())
                        .and(output3.mintOutputBuilder())
                        .and(output4.mintOutputBuilder())
                        .buildInputs(InputBuilders.createFromSender(senderAddress, senderAddress))
                        .andThen(CollateralBuilders.collateralFrom(collateral, collateralIndex))
                        .andThen(MintCreators.mintCreator(mintScript, multiAsset))
                        .andThen(ScriptCallContextProviders.scriptCallContext(mintScript, null, null, redeemerData, RedeemerTag.Mint, exUnits))
                        .andThen(AuxDataProviders.metadataProvider(metadata))
                        .andThen(FeeCalculators.feeCalculator(senderAddress, 1))
                        .andThen(ChangeOutputAdjustments.adjustChangeOutput(senderAddress, 1));

        TxSigner signer = SignerProviders.signerFrom(sender);

        Transaction signTxn = TxBuilderContext.init(backendService)
                .buildAndSign(txBuilder, signer);

        System.out.println(signTxn);
        Result<String> result = transactionService.submitTransaction(signTxn.serialize());
        System.out.println(result);
        waitForTransactionHash(result);
    }
}
