package versioncreator.popup.dialog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class VersionCreatorDialog extends Dialog{
	//待打版本的代码路径(.java)
	private Text text;
	//待打版本的项目名称
	private String projectName;
	//待保存版本压缩包的目标路 径
	private Text targetText;
	//窗口shell
	private Shell shell;
	//信息输出组件
	private Text outLab;
	//项目路径与代码路径衔接的中间路径，例如: 项目路径/WebRoot/WEB-INF/classes/源代码路径
	private String tempVersion;
	//项目资源，供重新编译代码用
	private IResource resource;
	//待打版本的代码路径(.class)
	private String nowProjectPath;
	//是否删除原CLASS文件选择框
	private Button check;
	//生成按钮
	private Button create;
	//刷新项目源文件的时间
	//private int step;
	//临时文件存放目录
	private String templatePath="D:";
	
	public VersionCreatorDialog(Shell parentShell,IResource resource) {
		super(parentShell);
		this.shell=parentShell;
		this.resource=resource;
		//格式  P/projectName
		this.projectName=resource.toString().substring(2);
	}
	

	//窗口部局
	@Override  
	protected Control createContents(Composite parent) {
		parent.getShell().setText("版本集成工具");
		Label lab=new Label(parent,SWT.NONE);
        lab.setText("输入类路径：");
	    text=new Text(parent,SWT.MULTI);
	    text.setLayoutData(new GridData(600,200));
	    Button btn=new Button(parent, SWT.NONE);
	    btn.setText("选择存放路径");
	    btn.setLayoutData(new GridData(90,-1));
	    btn.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent arg0) {
				FileDialog fd=new FileDialog(shell);
				SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd");
				String name=sdf.format(new Date());
				fd.setFileName("V1.0["+name+"-001]"+projectName+".rar");
				fd.open();
				targetText.setText(fd.getFilterPath()+File.separator+fd.getFileName());
			}
		});
	    targetText=new Text(parent, SWT.NONE);
	    targetText.setLayoutData(new GridData(600,-1));
	    outLab=new Text(parent,SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
	    outLab.setLayoutData(new GridData(580,150));
	    //让信息输出窗口焦点自动定位到最新输出信息处
	    outLab.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				outLab.setSelection(outLab.getText().length(), outLab.getText().length());
			}
		});
	    check=new Button(parent, SWT.CHECK);
	    check.setText("是否重新编译源代码");
	    check.setSelection(true);
	    check.addMouseListener(new MouseAdapter() {
	    	@Override
	    	public void mouseDown(MouseEvent e) {
	    		// TODO Auto-generated method stub
	    		if(check.getSelection()){
	    			create.setText("生成版本");
	    		}else{
	    			create.setText("重新编译");
	    		}
	    	}
		});
        return super.createContents(parent); 
	 }
	//打版本按钮点击事件
	@Override
	protected void cancelPressed() {
		if(!check.getSelection()){
			outLab.setText("[info]正在生成版本……");
		}
		//获取eclipse工作空间的所有项目
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		 IProject[] projects=root.getProjects();
		 for(IProject project : projects){
			 //如果是待打版本的项目
			 if(project.getName().equals(projectName)){
				 //设置待打版本的项目绝对路径
				 nowProjectPath=project.getLocation().toString();
			 }
		 }
		 File f=new File(nowProjectPath);
		 File[] fs=f.listFiles();
		 boolean flag=false;
		 //遍历该项目根目录(判断是WebRoot还是WebContent还是bin)
		 for(File temp : fs){
			 if("WebRoot".equals(temp.getName())){
				 flag=true;
				 nowProjectPath=nowProjectPath+File.separator+"WebRoot"+File.separator+"WEB-INF"+File.separator+"classes";
				 tempVersion="WEB-INF"+File.separator+"classes";
				 break;
			 }else if("WebContent".equals(temp.getName())){
				 flag=true;
				 nowProjectPath=nowProjectPath+File.separator+"WebContent"+File.separator+"WEB-INF"+File.separator+"classes";
				 tempVersion="WEB-INF"+File.separator+"classes";
				 break;
			 }else if("bin".equals(temp.getName())){
				 flag=true;
				 nowProjectPath=nowProjectPath+File.separator+"bin";
				 tempVersion="bin";
				 break;
			 }
		 }
		 //如果是WEB项目
		 if(flag){
			 //如果需要重新编译源代码
			 if(check.getSelection()){
				String versions=text.getText();
				String[] versionPath=versions.split("\r");
			    //删除原编译文件(待重新编译)
			    for(String path : versionPath){
			      //去掉tab、空格等特殊内容
				  path=path.replace("\r", "")
						  .replace("\n", "")
						  .replace("\t", "")
						  .replace(" ", "")
						  .replace("\\", ".")
						  .replace("/", ".")
						  .replace(".java", "")
						  .replace(".JAVA", "")
						  .replace(".class", "")
						  .replace(".CLASS", "");
				  if(path.length()==0){
					  continue;
				  }
				  //class源文件
				  File scrFile=new File(nowProjectPath+File.separator+path.replace(".", File.separator)+".class");
				  //java源文件
				  File fromFile=new File(f.getAbsolutePath()+File.separator+"src"+File.separator+path.replace(".", File.separator)+".java");
				  //如果java源文件不存在
				  if(!fromFile.exists()){
					  outLab.setText(outLab.getText()+"\r\n[error]源文件不存在["+path+".java"+"]");
					  return;
				  }
				  //如果class源文件存在则删除(待重新编译)
				  if(scrFile.exists()){
					  for(File temp : scrFile.getParentFile().listFiles()){
						  if(temp.isFile()&&temp.getName().contains(scrFile.getName().substring(0, scrFile.getName().indexOf("."))+"$")){
							  temp.delete();
						  }
					  }
					  scrFile.delete();
				  }
			    }
			    //新起一个线程重新编译项目文件，解决编译完成后刷新项目能实时同步在主UI线程中输出相应信息
			    try {
			    	new Thread(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							try {
								reBuilder();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}).start();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			 }else{
				 createVersion();
			 }
		 }else{
			 outLab.setText(outLab.getText()+"\r\n[error]非标准web工程(不存在WebRoot或者WebContent目录)");
		 }
		
	}
	
	//重新编译项目源文件
	private void reBuilder(){
		try {
			//重新编译项目源文件
			resource.getProject().build(IncrementalProjectBuilder.CLEAN_BUILD, new IProgressMonitor() {
				@Override
				public void done() {
					try {
						// TODO Auto-generated method stub
						//因为是在新的线程里编译的项目，要调ui主线程中的组件，所有要与主ui主线程同步执行
						Display.getDefault().syncExec(new Runnable() {
						    public void run() {
						    	outLab.setText(outLab.getText()+"\r\n[info]正在重新编译编码源代码……");
						    }
						 });
						//编译项目后的默认项目刷新时间10s
						/*step=10;
						while(step!=0){
							Display.getDefault().syncExec(new Runnable() {
							    public void run() {
							    	outLab.setText(outLab.getText()+"\r\n[info]"+step--+"!");
							    }
							 });
							Thread.sleep(1000);
						}
						Display.getDefault().syncExec(new Runnable() {
						    public void run() {
						    	outLab.setText(outLab.getText()+"\r\n[info]编译编码源代码成功……");
						    }
						 });*/
					} catch (Exception e) {
						e.printStackTrace();
					}
					//编译完项目后开发打包版本
					/*Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							// TODO Auto-generated method stub
							createVersion();
						}
					});*/
				}
				@Override
				public void worked(int arg0) {
					// TODO Auto-generated method stub
				}
				
				@Override
				public void subTask(String arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void setTaskName(String arg0) {
					// TODO Auto-generated method stub
				}
				
				@Override
				public void setCanceled(boolean arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public boolean isCanceled() {
					// TODO Auto-generated method stub
					return false;
				}
				
				@Override
				public void internalWorked(double arg0) {
					// TODO Auto-generated method stub
				}
				
				@Override
				public void beginTask(String arg0, int arg1) {
					// TODO Auto-generated method stub
				}
			});
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//打包版本
		private void createVersion(){
			String versions=text.getText();
			String[] versionPath=versions.split("\r");
			//清除历史版本文件夹
		    File delFile=new File(templatePath+File.separator+projectName);
		    deleteFile(delFile);
			try {
				  //循环各JAVA源代码路径
				  for(String path : versionPath){
					  path=path.replace("\r", "")
							  .replace("\n", "")
							  .replace("\t", "")
							  .replace(" ", "")
							  .replace("\\", ".")
							  .replace("/", ".")
							  .replace(".java", "")
							  .replace(".JAVA", "")
							  .replace(".class", "")
							  .replace(".CLASS", "");
					  if(path.length()==0){
						  continue;
					  }
					  //获取JAVA源代码包路径
					  String p=path.substring(0,path.lastIndexOf(".")).replace(".",File.separator);
					  //创建JAVA目标源代码
					  String str=nowProjectPath+File.separator+path.replace(".", File.separator)+".class";
					  File fromFile=new File(str);
					  //创建JAVA源代码路径
					  File toFilePath=new File(templatePath+File.separator+projectName+File.separator+tempVersion+File.separator+p);
					  //重新创建文件夹
					  toFilePath.mkdirs();
					  //创建JAVA待生成的源代码
					  File toFile=new File(templatePath+File.separator+projectName+File.separator+tempVersion+File.separator+path.replace(".", File.separator)+".class");
					  if(!toFile.exists())
						  toFile.createNewFile();
					  //将目标源代码COPY到目标源代码
					  this.copyFile(fromFile, toFile);
					  //COPY目标子源代码$1、$1$2之类的
					  File fromPath=new File(nowProjectPath+File.separator+p);
					  for(File f : fromPath.listFiles()){
						  if(f.isFile()&&f.getName().contains(fromFile.getName().substring(0, fromFile.getName().indexOf("."))+"$")){
							  String tempPath=templatePath+File.separator+projectName+File.separator+tempVersion+File.separator+p+File.separator+f.getName();
							  File toFile1=new File(tempPath);
							  if(!toFile1.exists())
								  toFile1.createNewFile();
							  this.copyFile(f, toFile1);
						  }
					  }
				  }
				  //如果存在该压缩包，先删除
				  File oFile=new File(targetText.getText());
				  if(oFile.exists())
					  oFile.delete();
				  //压缩目标源代码到rar版本包
				  outLab.setText(outLab.getText()+"\r\n[info]正在打包……");
				  String targetFile=targetText.getText();
				  if(targetFile.length()<2){
					  outLab.setText(outLab.getText()+"\r\n[error]无效的存放路径……");
					  return;
				  }
				  String targetPath=targetFile.substring(0, targetFile.lastIndexOf("\\")-1);
				  File targetPathFile=new File(targetPath);
				  try{
					  targetPathFile.mkdirs();
				  }catch(Exception e){
					  e.printStackTrace();
					  outLab.setText(outLab.getText()+"\r\n[error]无效的存放路径……");
					  return;
				  }
				  //调用rar命令打包
				  Process process=Runtime.getRuntime().exec("cmd.exe /c rar a " + targetFile+" "+templatePath+File.separator+projectName);
				  InputStream fis = process.getInputStream(); 
	              BufferedReader br = new BufferedReader(new InputStreamReader(fis,"gbk")); 
	              String line = null;
	              //将rar打包输出信息打印到前台组件
	              while ((line = br.readLine()) != null) { 
	            	  outLab.setText(outLab.getText()+"\r\n[info]"+line);
	              } 
				  outLab.setText(outLab.getText()+"\r\n\r\n[info]Success!");
			  } catch (Exception e) {
				  e.printStackTrace();
				  outLab.setText(outLab.getText()+"\r\n[error]系统错误:"+e.getMessage());
			  }
		}
	@Override
	protected void initializeBounds() {
		// TODO Auto-generated method stub
		Button ok=super.getButton(IDialogConstants.OK_ID);
		ok.setVisible(false);
		create=super.getButton(IDialogConstants.CANCEL_ID);
		create.setText("重新编译");
		super.initializeBounds();
	}
	//递归删除文件夹下面的所有文件 
		private void deleteFile(File file)
		  {
		    if (file.exists())
		      for (File temp : file.listFiles()) {
		        if (!temp.isFile()) {
		          deleteFile(temp);
		        }
		        temp.delete();
		      }
		  }
		//复制文件
		private void copyFile(File sourceFile, File targetFile) throws IOException {
		       BufferedInputStream inBuff = null;
		       BufferedOutputStream outBuff = null;
		       try {
		           // 新建文件输入流并对它进行缓冲
		          inBuff = new BufferedInputStream(new FileInputStream(sourceFile));
		           // 新建文件输出流并对它进行缓冲
		          outBuff = new BufferedOutputStream(new FileOutputStream(targetFile));
		           // 缓冲数组
		          byte[] b = new byte[1024 * 5];
		          int len;
		          while ((len = inBuff.read(b)) != -1) {
		              outBuff.write(b, 0, len);
		          }
		          // 刷新此缓冲的输出流
		          outBuff.flush();
		       } finally {
		           // 关闭流
		          if (inBuff != null)
		              inBuff.close();
		          if (outBuff != null)
		              outBuff.close();
		       }
		   }
}
