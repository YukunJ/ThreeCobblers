package edu.cmu.cc.webtier;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonArray;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.InflaterOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The Block Chain Validator for Micro-service 2
 */
public class BlockChainValidator {

	enum Transaction {
		INVALID, OTHER, MINER, REWARD;
	}
	
    private HashMap<String, Long> balance = new HashMap<>();

	private static final String MY_ACCOUNT = "1097844002039";

	private static final long TENMINUTES = 600000000000L;

	private long valid_reward = 1000000000;

	// tx1_hash|tx2_hash|tx3_hash
	private String all_new_tx_hashes = null;

	private String new_pow = null;

	private String new_block_hash = null;
	
	private Long prev_timestamp = 0L;

	private int prev_blockid = -1;
	
	private String prev_blockhash = "00000000";

	/**
	The main driver of validating a request URL
	*/
	public String validateRequest(String rawRequest) throws Exception {
		// Todo change to environment variables
		String team_id = "ThreeCobblers";
		String team_aws_id = "971734603674";
		String message = team_id + "," + team_aws_id + "\n";
		JsonObject request = UtilsBlock.decompress(rawRequest);
		JsonArray chain = request.getAsJsonArray("chain");
		if (!validateChain(chain)) {
			message = message + "INVALID";
			return message;
		}
		long prev_block_time = this.prev_timestamp;
		JsonArray transactions = request.get("new_tx").getAsJsonArray();
		transactions = this.verifyNewTransactions(transactions);
		if (transactions == null) {
			message = message + "INVALID";
			return message;
		}
		
		String new_target = request.get("new_target").getAsString();
		JsonObject reward = mineTheReward(new_target, prev_block_time);
		if (reward == null) {
			message = message + "INVALID";
			return message;
		}
		
		JsonObject new_block = this.createNewBlock(transactions, reward, new_target);
		chain.add(new_block);

		JsonObject returnChain = new JsonObject();
		returnChain.add("chain", chain);
		return message + UtilsBlock.compress(returnChain);
	}

    public boolean validateChain(JsonArray chain) {
    	for (int i = 0; i < chain.size(); i++) {
  			if (!validateBlock(chain.get(i).getAsJsonObject())) {
  				return false;
  			}
  		}
    	return true;
    }
  
    public boolean validateBlock(JsonObject block) {
        // Things to validate:
		//-1. All blocks ID should be continous increasing Integer, and halve the reward if needed
		// 0. Each transaction contains necessary fields
        // 1. Only and must the last transaction is the reward
        // 2. All transations in time-increasing order
        // 3. The receiver/sender's balance must be non-negative
        // 4. For real transaction, verify the signature
        // 5. For real transaction, veryify the CCHash match
		// 6. For reward transaction, verify the reward amount is correct
		// 7. Veryify the Block Hash match
		// 8. Veryify the Hash is smaller than the target
		ArrayList<String> transactions_hash = new ArrayList<>();
		JsonArray all_transactions = block.get("all_tx").getAsJsonArray();
		String pow = block.get("pow").getAsString();
		int block_id = block.get("id").getAsInt();
		String blockhash = block.get("hash").getAsString();
		String target = block.get("target").getAsString();
		Long cumulative_fee = 0L;

		// -1. All blocks ID should be continous increasing Integer, and halve the reward if needed
		if (!(block_id == prev_blockid + 1)) {
      		return false;
		}
		if (block_id % 2 == 0) {
			valid_reward /= 2;
		}
		this.prev_blockid = block_id;
		
		for (int i = 0; i < all_transactions.size(); i++) {
			JsonObject transaction = all_transactions.get(i).getAsJsonObject();

			// 0. Each transaction contains necessary fields
			Transaction type = getTransactionType(transaction);
			if ((!(type == Transaction.OTHER)) && (!(type == Transaction.REWARD))) {
				return false;
			}
		

			// 1. Only and must the last transaction is the reward
			boolean reward_condition = (i == (all_transactions.size() - 1)) && (type == Transaction.REWARD);
			boolean regular_condition = (i != (all_transactions.size() - 1)) && (type == Transaction.OTHER);
			if (!(reward_condition || regular_condition)) {
				return false;
			}
			
			// 2. All transations in time-increasing order
			Long timestamp = transaction.get("time").getAsLong();
			if (!(prev_timestamp < timestamp)) {
				return false;
			}
			this.prev_timestamp = timestamp;


			String receiver = transaction.get("recv").getAsString();
			Long amount = transaction.get("amt").getAsLong();
			String time = transaction.get("time").getAsString();
			String hash = transaction.get("hash").getAsString();
			transactions_hash.add(hash);
			
			// 3. The receiver/sender's balance must be non-negative
			if (amount < 0) { return false; }
			if (regular_condition) {
				String signature = transaction.get("sig").getAsString();
				String sender = transaction.get("send").getAsString();
				Long fee = transaction.get("fee").getAsLong();
				if (fee < 0) { return false; }
				if (balance.getOrDefault(sender, (long)0) < (amount + fee)) {
					return false;
				}
				cumulative_fee += fee;
				balance.put(sender, balance.get(sender) - amount - fee);
				balance.put(receiver, balance.getOrDefault(receiver, (long)0) + amount);

				// 4. For real transaction, verify the signature
				if (!UtilsBlock.validateSignature(signature, hash, sender)) {
					return false;
				}
				
				// 5. For real transaction, veryify the CCHash match
				String to_be_hashed = time + "|" + sender + "|" + receiver + "|" + amount.toString() + "|" + fee.toString();
				String computed_hash = UtilsBlock.CCHash(to_be_hashed);
				if (!computed_hash.equals(hash)) {
					return false;
				}
				
			} else {
				// pure reward transaction
				// 6. For reward transaction, verify the reward amount is correct
				if (amount != valid_reward) {
					return false;
				}
				
				balance.put(receiver, balance.getOrDefault(receiver, (long)0) +  amount + cumulative_fee);
				// also check the reward transaction's hash
				String to_be_hashed = time + "|" + "" + "|" + receiver + "|" + amount.toString() + "|" + "";
				String computed_hash = UtilsBlock.CCHash(to_be_hashed);
				if (!computed_hash.equals(hash)) {
					return false;
				}
			}
			
		}

		// 7. Veryify the Block Hash match
		String to_be_256hashed = block_id + "|" + prev_blockhash + "|" + String.join("|", transactions_hash);
		String computed_hash = UtilsBlock.CCHash(UtilsBlock.Sha256(to_be_256hashed) + pow);
		if (!computed_hash.equals(blockhash)) {
			return false;
		}
		this.prev_blockhash = blockhash;

		// 8. Veryify the Hash is smaller than the target
		if (!(blockhash.compareTo(target) < 0)) {
			return false;
		}
		
      	return true;
    }

    /**
     * Decode and Decompress the raw request string
     * @param raw_request : the raw zlib 64 url-safe encoded request string
     * @return the json-like string after decoding and decompression
     * @throws Exception
     */
    

    public static void main(String[] args) throws Exception {
        String base64_encoded_request = "eJyFkNFqwzAMRf9Fz36QbMu28iujFCdVm0CbjSa0g5J_n1PMsnUP05O5V-Ye3Qd0fR5GaN4ekM_n_fz5fF61u0FDKDF5j2jRiYF8maFhrGNgHi4KDRAnyxY9bgMG-jz1xbSibRedwLIz8PF-L5J3xR4O0OD3FpKkI0ctxpyvJ51XTWExv6Cm4VSYXOKUAllL3psKGgN7ocAJyRk4asHySZjYV2oXUnRuPeIFmjgm5iTIvoRPOh7-nF0ZqZMuaHZPqhprJQhHizHSf_WISMCSYgP6rR5ER5Kd_qiHqNZD21ZUbjm3r_WUP6Pe95smUJW1sN3yBfIbe98=";
		BlockChainValidator bcv = new BlockChainValidator();
		System.out.println(bcv.validateRequest(base64_encoded_request));
    }

	/**
     * @param transaction: a potentioal transaction, can be in one of four types
     * @return integer representing type of transaction
     */
	private static Transaction getTransactionType(JsonObject transaction) {
		int numFields = transaction.size();
		if (numFields == 3) {
			// corresponding to new transaction from miner
			if (isMinerTransaction(transaction)) {
				return Transaction.MINER;
			} else {
				return Transaction.INVALID;
			}
		} else if (numFields == 4) {
			// corresponding to reward transaction
			if (isRewardTransaction(transaction)) {
				return Transaction.REWARD;
			} else {
				return Transaction.INVALID;
			}
		} else if (numFields == 7) {
			// corresponding to transaction made by other people. Can be new or old
			if (isOtherTransaction(transaction)) {
				return Transaction.OTHER;
			} else {
				return Transaction.INVALID;
			}
		} else {
			// invalid transaction
			return Transaction.INVALID;
		}
	}

	/**
     * @param transaction: a potentioal new transaction from miner
     * @return boolean representing whether the transaction is new transaction from miner
     */
	private static boolean isMinerTransaction(JsonObject transaction) {
		return (transaction.has("recv") && transaction.has("amt") && transaction.has("time"));
	}

	/**
     * @param transaction: a potentioal reward transaction
     * @return boolean representing whether the transaction reward transaction
     */
	private static boolean isRewardTransaction(JsonObject transaction) {
		return (transaction.has("recv") && transaction.has("amt") 
				&& transaction.has("time") && transaction.has("hash"));
	}

	/**
     * @param transaction: a potentioal transaction from other e
     * @return boolean representing whether the transaction is new transaction from miner
     */
	private static boolean isOtherTransaction(JsonObject transaction) {
		return (transaction.has("sig") && transaction.has("recv") 
				&& transaction.has("fee") && transaction.has("amt")
			    && transaction.has("time") && transaction.has("send")
			    && transaction.has("hash"));
	}

	/**
     * @param transactions: new transactions for which we need to verify
     * @return JsonObject representing the new block
     */
	private JsonArray verifyNewTransactions(JsonArray new_transactions) {
		ArrayList<String> transactions_hash = new ArrayList<>();
		JsonArray updatedTransactions = new JsonArray();
		for (int i = 0; i < new_transactions.size(); i++) {
			JsonObject transaction = new_transactions.get(i).getAsJsonObject();

			// 0. Each transaction contains necessary fields
			Transaction type = getTransactionType(transaction);
			if ((!(type == Transaction.OTHER)) && (!(type == Transaction.MINER))) {
				return null;
			}
		

			// 1. Only and must the last transaction is the reward
			boolean miner_condition = (type == Transaction.MINER);
			boolean regular_condition = (type == Transaction.OTHER);
			
			if (!(miner_condition || regular_condition)) {
				return null;
			}
			
			// 2. All transations in time-increasing order
			Long timestamp = transaction.get("time").getAsLong();
			if (!(prev_timestamp < timestamp)) {
				return null;
			}
			this.prev_timestamp = timestamp;


			String receiver = transaction.get("recv").getAsString();
			Long amount = transaction.get("amt").getAsLong();
			String time = transaction.get("time").getAsString();
			
			// 3. The receiver/sender's balance must be non-negative
			if (amount < 0) { return null; }
			if (regular_condition) {
				String hash = transaction.get("hash").getAsString();
			    transactions_hash.add(hash);
				String signature = transaction.get("sig").getAsString();
				String sender = transaction.get("send").getAsString();
				Long fee = transaction.get("fee").getAsLong();
				if (fee < 0) { return null; }
				if (balance.getOrDefault(sender, (long)0) < (amount + fee)) {
					return null;
				}
				
				balance.put(sender, balance.get(sender) - amount - fee);
				balance.put(receiver, balance.getOrDefault(receiver, (long)0) + amount);

				// 4. For real transaction, verify the signature
				if (!UtilsBlock.validateSignature(signature, hash, sender)) {
					return null;
				}
				
				// 5. For real transaction, veryify the CCHash match
				String to_be_hashed = time + "|" + sender + "|" + receiver + "|" + amount.toString() + "|" + fee.toString();
				String computed_hash = UtilsBlock.CCHash(to_be_hashed);
				if (!computed_hash.equals(hash)) {
					return null;
				}
				
			} else {
                // need to add fields and add to updatedTransactions
				Long fee = 0L;
				String sender = MY_ACCOUNT;
				String toBeHashed = time + "|" + sender + "|" + receiver + "|" + amount.toString() + "|" + fee.toString();
				String hash = UtilsBlock.CCHash(toBeHashed);
				long signature = UtilsBlock.generateSignature(hash);
				
				transaction.addProperty("fee", fee);
				transaction.addProperty("send", Long.parseLong(sender));
				transaction.addProperty("sig", signature);
				transaction.addProperty("hash", hash);

			    transactions_hash.add(hash);

				if (balance.getOrDefault(sender, (long)0) < (amount + fee)) {
					return null;
				}
				
				balance.put(sender, balance.get(sender) - amount - fee);
				balance.put(receiver, balance.getOrDefault(receiver, (long)0) + amount);
			}
			updatedTransactions.add(transaction);
		}
		this.all_new_tx_hashes = String.join("|", transactions_hash);
		return updatedTransactions;
	}

	private static boolean isHashSmaller(String myHash, String target) {
		return myHash.compareTo(target) < 0;
	}

	private String sha() {
		// SHA-256("block_id|previous_block_hash|tx1_hash|tx2_hash|tx3_hash...")
		int blockid = this.prev_blockid + 1;
		String input = String.valueOf(blockid) + "|" + this.prev_blockhash + "|" + this.all_new_tx_hashes;
		return UtilsBlock.Sha256(input);
	}
	
	private JsonObject mineTheReward(String newTarget, long prev_block_time) {
		// bingo, find pow, create the transacion for reward
				
		// new time stamp, needs to guarantee prev_timestamp is the reward time of last block
		long timeStamp = TENMINUTES + prev_block_time;

		// find the reward amount
		int blockid = this.prev_blockid + 1;
		long amt;
		if (blockid % 2 == 0) {
			amt = valid_reward / 2;
		} else {
			amt = valid_reward;
		}

		// we are the miner, so the recevier should be us
		String recv = MY_ACCOUNT;

		// compute the transaction hash as CCHash("timestamp|sender|recipient|amount|fee")
		String toBeHashed = String.valueOf(timeStamp) + "||" + recv + "|" + String.valueOf(amt) + "|";
		String hash = UtilsBlock.CCHash(toBeHashed);

		// create the actual transaction representing the reward transaction.
		JsonObject rewardTransaction = new JsonObject();
		rewardTransaction.addProperty("recv", Long.parseLong(recv));
		rewardTransaction.addProperty("amt", amt);
		rewardTransaction.addProperty("time", String.valueOf(timeStamp));
		rewardTransaction.addProperty("hash", hash);

		if (this.all_new_tx_hashes.length() == 0) {
			this.all_new_tx_hashes = hash;
		} else {
			this.all_new_tx_hashes = this.all_new_tx_hashes + "|" + hash;
		}
		
		String sha = this.sha();
		// Todo: modify the upper bound of the loop
		// the actual mining process
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			String pow = UtilsBlock.pow(i);
			String blockHash = UtilsBlock.CCHash(sha + pow);
			if (isHashSmaller(blockHash, newTarget)) {
				this.new_block_hash = blockHash;
				this.new_pow = pow;
				
				return rewardTransaction;
			}
		}
		return null;
	}

	/**
     * @param transactions: new transactions for which we need to create a new block
     * @param reward: the reward transaction to the miner
     * @return JsonObject representing the new block
     */
	private JsonObject createNewBlock(JsonArray updatedTransactions, JsonObject reward, String newTarget) {
		updatedTransactions.add(reward);
		// Create new block
		JsonObject newBlock = new JsonObject();
		newBlock.add("all_tx", updatedTransactions);
		newBlock.addProperty("id", this.prev_blockid + 1);
		newBlock.addProperty("pow", this.new_pow);
		newBlock.addProperty("hash", this.new_block_hash);
		newBlock.addProperty("target", newTarget);
		return newBlock;
	}

}
