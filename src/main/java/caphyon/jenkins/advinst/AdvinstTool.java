/*
 * The MIT License
 *
 * Copyright 2015 Ciprian Burca.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package caphyon.jenkins.advinst;

import java.io.IOException;
import java.util.List;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.util.ArgumentListBuilder;

public final class AdvinstTool {
  private final String mAdvinstComPath;

  public AdvinstTool(final String advinstComPath) {
    this.mAdvinstComPath = advinstComPath;
  }

  public boolean executeCommands(final List<String> commands, final FilePath aipPath, final FilePath workspace,
      final Launcher launcher, final TaskListener listener, final EnvVars env) throws AdvinstException {
    FilePath aicFilePath = null;
    try {
      if (launcher.isUnix()) {
        throw new AdvinstException(Messages.ERR_ADVINST_UNSUPPORTED_OS());
      }

      FilePath pwd = workspace;
      if (null == pwd) {
        return false;
      }

      aicFilePath = createAicFile(pwd, commands);
      if (null == aicFilePath) {
        throw new AdvinstException(Messages.ERR_ADVINST_FAILED_AIC());
      }

      ArgumentListBuilder cmdExecArgs = new ArgumentListBuilder();
      cmdExecArgs.add(mAdvinstComPath, "/execute", aipPath.getRemote(), aicFilePath.getRemote());

      int result = launcher.launch().cmds(cmdExecArgs).envs(env).stdout(listener).pwd(pwd).join();
      return 0 == result;

    } catch (IOException e) {
      throw new AdvinstException(e);
    } catch (InterruptedException e) {
      throw new AdvinstException(e);
    } finally {
      try {
        if (aicFilePath != null) {
          aicFilePath.delete();
        }
      } catch (IOException e) {
        throw new AdvinstException(e);
      } catch (InterruptedException e) {
        throw new AdvinstException(e);
      }
    }
  }

  private static FilePath createAicFile(final FilePath buildWorkspace, final List<String> aCommands)
      throws IOException, InterruptedException {
    FilePath aicFile = buildWorkspace.createTempFile("aic", "aic");
    StringBuffer fileContent = new StringBuffer(AdvinstConsts.AdvinstAicHeader + "\r\n");
    for (String command : aCommands) {
      fileContent.append(command);
      fileContent.append("\r\n");
    }

    aicFile.write(fileContent.toString(), "UTF-16");
    return aicFile;
  }
}
