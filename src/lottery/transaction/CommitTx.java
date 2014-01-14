package lottery.transaction;

import java.math.BigInteger;
import java.util.List;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ScriptException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.bitcoin.script.ScriptOpCodes;

public class CommitTx extends LotteryTx {
	byte[] hash;
	byte[] commitPk;
	int minLength;
	int noPlayers;

	public CommitTx(TransactionOutput out, ECKey sk, List<byte[]> pks, int position,
			byte[] hash, int minLength, BigInteger fee, boolean testnet) throws ScriptException {
		this.hash = hash;
		this.commitPk = sk.getPubKey();
		this.minLength = minLength;
		this.noPlayers = pks.size();
		NetworkParameters params = getNetworkParameters(testnet);
		BigInteger stake = out.getValue().subtract(fee).divide(BigInteger.valueOf(noPlayers-1));
		
		tx = new Transaction(params);
		tx.addInput(out); //TODO: validate !
		if (out.getScriptPubKey().isSentToAddress()) {
			tx.getInput(0).setScriptSig(ScriptBuilder.createInputScript(sign(0, sk), sk));
		}
		else if (out.getScriptPubKey().isSentToAddress()) {
			tx.getInput(0).setScriptSig(ScriptBuilder.createInputScript(sign(0, sk)));
		} 
		else {
			throw new RuntimeException("bad TransactionOutput!"); //TODO
		}
		for (int k = 0; k < noPlayers; ++k) {
			BigInteger currentStake = stake;
			if (k == position) { //TODO: simplier script?
				currentStake = new BigInteger("0");
			}
			tx.addOutput(currentStake, getCommitOutScript(pks.get(k)));
		}
	}
	
	protected Script getCommitOutScript(byte[] receiverPk) {
		byte[] min = Utils.parseAsHexOrBase58(Integer.toHexString(minLength));
		byte[] max = Utils.parseAsHexOrBase58(Integer.toHexString(minLength+noPlayers));
		return new ScriptBuilder()
				.op(ScriptOpCodes.OP_SIZE)
				.data(min)
				.data(max)
				.op(ScriptOpCodes.OP_WITHIN)
				.op(ScriptOpCodes.OP_SWAP)
				.op(ScriptOpCodes.OP_SHA256) //TODO
				.data(hash)
				.op(ScriptOpCodes.OP_EQUAL)
				.op(ScriptOpCodes.OP_BOOLAND)
				.op(ScriptOpCodes.OP_SWAP)
				.data(receiverPk)			//TODO: only hash? !
				.op(ScriptOpCodes.OP_CHECKSIG)
				.op(ScriptOpCodes.OP_BOOLOR)
				.op(ScriptOpCodes.OP_VERIFY)
				.data(commitPk)
				.op(ScriptOpCodes.OP_CHECKSIG)
				.build();
	}

}