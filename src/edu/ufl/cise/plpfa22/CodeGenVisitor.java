package edu.ufl.cise.plpfa22;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import edu.ufl.cise.plpfa22.ast.ASTVisitor;
import edu.ufl.cise.plpfa22.CodeGenUtils.GenClass;
import edu.ufl.cise.plpfa22.IToken.Kind;
import edu.ufl.cise.plpfa22.ast.Block;
import edu.ufl.cise.plpfa22.ast.ConstDec;
import edu.ufl.cise.plpfa22.ast.ExpressionBinary;
import edu.ufl.cise.plpfa22.ast.ExpressionBooleanLit;
import edu.ufl.cise.plpfa22.ast.ExpressionIdent;
import edu.ufl.cise.plpfa22.ast.ExpressionNumLit;
import edu.ufl.cise.plpfa22.ast.ExpressionStringLit;
import edu.ufl.cise.plpfa22.ast.Ident;
import edu.ufl.cise.plpfa22.ast.ProcDec;
import edu.ufl.cise.plpfa22.ast.Program;
import edu.ufl.cise.plpfa22.ast.Statement;
import edu.ufl.cise.plpfa22.ast.StatementAssign;
import edu.ufl.cise.plpfa22.ast.StatementBlock;
import edu.ufl.cise.plpfa22.ast.StatementCall;
import edu.ufl.cise.plpfa22.ast.StatementEmpty;
import edu.ufl.cise.plpfa22.ast.StatementIf;
import edu.ufl.cise.plpfa22.ast.StatementInput;
import edu.ufl.cise.plpfa22.ast.StatementOutput;
import edu.ufl.cise.plpfa22.ast.StatementWhile;
import edu.ufl.cise.plpfa22.ast.Types.Type;
import edu.ufl.cise.plpfa22.ast.VarDec;

//state and express pass mv else cw

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	final String packageName;
	final String className;
	final String sourceFileName;
	final String fullyQualifiedClassName; 
	final String classDesc;
	Boolean prePass;
	String currClassAdd;
	List<GenClass> recs;
	Node root;
	Node curr;

	private class Node{
		final String cname;
		final ProcDec dec;
		List<Node> children;
		final Node parent;

		public Node(Node parent, String cname, ProcDec procDec){
			this.cname = cname;
			this.parent = parent;
			this.children = new ArrayList<Node>();
			this.dec = procDec;
		}

	}
	
	public CodeGenVisitor(String className, String packageName, String sourceFileName) {
		super();
		this.packageName = packageName;
		this.className = className;
		this.sourceFileName = sourceFileName;
		this.fullyQualifiedClassName = packageName + "/" + className;
		this.classDesc="L"+this.fullyQualifiedClassName+';';
		this.prePass = true;
		this.currClassAdd = "";
		this.recs = new ArrayList<GenClass>();
		this.root = null;
		this.curr = null;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws PLPException {
		if(this.prePass){
			String cn = (String) arg;
			for (ProcDec procDec: block.procedureDecs) {
				String thisAlone = String.valueOf(procDec.ident.getText());
				cn += "$" + thisAlone;
				Node temp = new Node(this.curr, thisAlone, procDec);
				this.curr.children.add(temp);
				this.curr = temp;
				procDec.visit(this, cn);
				this.curr = temp.parent;
				cn = cn.substring(0, cn.length() - thisAlone.length() - 1);
			}
		}
		else{
			ClassWriter cw = (ClassWriter) arg;
			for (ConstDec constDec : block.constDecs) {
				constDec.visit(this, null);
			}
			for (VarDec varDec : block.varDecs) {
				varDec.visit(this, cw);
			}
			for (ProcDec procDec: block.procedureDecs) {
				Node temp = null;
				for(int i = 0; i < this.curr.children.size(); i++){
					if(this.curr.children.get(i).dec.getClassName().equals(procDec.getClassName())){
						temp = this.curr.children.get(i);
						break;
					}
				}
				this.curr = temp;
				procDec.visit(this, null);
				this.curr = this.curr.parent;
			}
			//add instructions from statement to method
			//get a method visitor for the run method.		
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null); 
			mv.visitCode(); 

			block.statement.visit(this, mv);

			mv.visitInsn(RETURN);
			mv.visitMaxs(0, 0); 
			mv.visitEnd(); 
		}
		return null;
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws PLPException {
		this.root = new Node(null, "", null);
		this.curr = this.root;
		program.block.visit(this, "");
		this.curr = this.root;
		this.prePass = false;

		//create a classWriter and visit it
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

		//Hint:  if you get failures in the visitMaxs, try creating a ClassWriter with 0
		// instead of ClassWriter.COMPUTE_FRAMES.  The result will not be a valid classfile,
		// but you will be able to print it so you can see the instructions.  After fixing,
		// restore ClassWriter.COMPUTE_FRAMES
		cw.visit(V18, ACC_PUBLIC | ACC_SUPER, fullyQualifiedClassName, null, "java/lang/Object", new String[] {"java/lang/Runnable"});

		cw.visitSource(this.sourceFileName, null); 
 
		//do all combos of single nest
		preorderNestings(this.root, cw);

		//-------------init---------------
		MethodVisitor mvi = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null); 
		mvi.visitCode(); 

		mvi.visitVarInsn(ALOAD, 0); 
		mvi.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false); 

		mvi.visitInsn(RETURN);

		mvi.visitMaxs(0, 0); 
		mvi.visitEnd(); 

		////-------------main---------------
		MethodVisitor mvm = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		mvm.visitCode(); 
		mvm.visitTypeInsn(NEW, this.fullyQualifiedClassName); 
		mvm.visitInsn(DUP); 
		mvm.visitMethodInsn(INVOKESPECIAL, this.fullyQualifiedClassName, "<init>", "()V", false); 
		mvm.visitMethodInsn(INVOKEVIRTUAL, this.fullyQualifiedClassName, "run", "()V", false); 
		mvm.visitInsn(RETURN); 
		mvm.visitMaxs(0, 0); 
		mvm.visitEnd(); 

		//visit the block, passing it the ClassWriter
		program.block.visit(this, cw);

		//finish up the class
		// mvm.visitInsn(RETURN);
		// mvm.visitMaxs(0,0);
		// mvm.visitEnd();

        cw.visitEnd();
        //return the bytes making up the classfile

		this.recs.add(new GenClass(this.fullyQualifiedClassName, cw.toByteArray()));
		Collections.reverse(recs);
		return recs;
	}

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws PLPException {
		statementAssign.expression.visit(this, arg);
		statementAssign.ident.visit(this, arg);
		return null;
	}

	@Override
	public Object visitVarDec(VarDec varDec, Object arg) throws PLPException {
		ClassWriter cw = (ClassWriter)arg;
		FieldVisitor fv;
		if(varDec.getType() == Type.BOOLEAN){
			fv = cw.visitField(0, String.valueOf(varDec.ident.getText()), "Z", null, null);
			fv.visitEnd();
		}
		else if(varDec.getType() == Type.NUMBER){
			fv = cw.visitField(0, String.valueOf(varDec.ident.getText()), "I", null, null);
			fv.visitEnd();
		}
		else if(varDec.getType() == Type.STRING){
			fv = cw.visitField(0, String.valueOf(varDec.ident.getText()), "Ljava/lang/String;", null, null);
			fv.visitEnd();
		}
		return null;
	}

	@Override
	public Object visitStatementCall(StatementCall statementCall, Object arg) throws PLPException {
		//fix the finding of the dec
		MethodVisitor mv = (MethodVisitor)arg;

		int at = (this.curr.dec == null ? 0 : this.curr.dec.getNest() + 1);
		int target = statementCall.ident.getDec().getNest();
		String targetName = ((ProcDec) statementCall.ident.getDec()).getClassName();
		int changes = at - target;

		String nest = this.fullyQualifiedClassName + targetName;
		String enc = "";

		mv.visitTypeInsn(NEW, nest); 
		mv.visitInsn(DUP); 
		mv.visitVarInsn(ALOAD, 0);

		if(changes == 0){
			enc = this.fullyQualifiedClassName + (this.curr.dec == null ? "" : this.curr.dec.getClassName());
		}
		else{
			Node temp = this.curr;
			for(int i = changes - 1; i > 0; i--){
				enc = this.fullyQualifiedClassName + (temp.parent.dec == null ? "" : temp.parent.dec.getClassName());
				mv.visitFieldInsn(GETFIELD, this.fullyQualifiedClassName + temp.dec.getClassName(), "this$" + String.valueOf(target + i), "L" + enc + ";"); 
				temp = temp.parent;
			}
			enc = this.fullyQualifiedClassName + (temp.parent.dec == null ? "" : temp.parent.dec.getClassName());
			mv.visitFieldInsn(GETFIELD, this.fullyQualifiedClassName + temp.dec.getClassName(), "this$" + String.valueOf(target), "L" + enc + ";"); 
		}

		mv.visitMethodInsn(INVOKESPECIAL, nest, "<init>", "(L" + enc + ";)V", false); 
		mv.visitMethodInsn(INVOKEVIRTUAL, nest, "run", "()V", false); 

		return null;
	}

	@Override
	public Object visitStatementInput(StatementInput statementInput, Object arg) throws PLPException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object visitStatementOutput(StatementOutput statementOutput, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor)arg;
		mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
		statementOutput.expression.visit(this, arg);
		Type etype = statementOutput.expression.getType();
		String JVMType = (etype.equals(Type.NUMBER) ? "I" : (etype.equals(Type.BOOLEAN) ? "Z" : "Ljava/lang/String;"));
		String printlnSig = "(" + JVMType +")V";
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", printlnSig, false);
		return null;
	}

	@Override
	public Object visitStatementBlock(StatementBlock statementBlock, Object arg) throws PLPException {
		List<Statement> statements = statementBlock.statements;
		for(Statement state: statements) {
			state.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitStatementIf(StatementIf statementIf, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor)arg;
		Label trueGuard = new Label();
		statementIf.expression.visit(this, arg);
		mv.visitJumpInsn(IFEQ, trueGuard);	
		statementIf.statement.visit(this, arg);
		mv.visitLabel(trueGuard);
		return null;
	}

	@Override
	public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor) arg;

		Label label0 = new Label();
		mv.visitJumpInsn(GOTO, label0);

		Label label1 = new Label();
		mv.visitLabel(label1);

		statementWhile.statement.visit(this,  arg);
		mv.visitLabel(label0);

		statementWhile.expression.visit(this,  arg);
		mv.visitJumpInsn(IFNE, label1);

		return null;
	}

	@Override
	public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor) arg;
		Type argType = expressionBinary.e0.getType();
		Kind op = expressionBinary.op.getKind();
		switch (argType) {
		case NUMBER -> {
			expressionBinary.e0.visit(this, arg);
			expressionBinary.e1.visit(this, arg);
			switch (op) {
			case PLUS -> mv.visitInsn(IADD);
			case MINUS -> mv.visitInsn(ISUB);
			case TIMES -> mv.visitInsn(IMUL);
			case DIV -> mv.visitInsn(IDIV);
			case MOD -> mv.visitInsn(IREM);
			case EQ -> {
				Label labelNumEqFalseBr = new Label();
				mv.visitJumpInsn(IF_ICMPNE, labelNumEqFalseBr);
				mv.visitInsn(ICONST_1);
				Label labelPostNumEq = new Label();
				mv.visitJumpInsn(GOTO, labelPostNumEq);
				mv.visitLabel(labelNumEqFalseBr);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(labelPostNumEq);
			}
			case NEQ -> {
				Label labelNumNeqFalseBr = new Label();
				mv.visitJumpInsn(IF_ICMPEQ, labelNumNeqFalseBr);
				mv.visitInsn(ICONST_1);
				Label labelPostNumNeq = new Label();
				mv.visitJumpInsn(GOTO, labelPostNumNeq);
				mv.visitLabel(labelNumNeqFalseBr);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(labelPostNumNeq);
			}
			case LT -> {
				Label labelNumLtFalseBr = new Label();
				mv.visitJumpInsn(IF_ICMPGE, labelNumLtFalseBr);
				mv.visitInsn(ICONST_1);
				Label labelPostNumLt = new Label();
				mv.visitJumpInsn(GOTO, labelPostNumLt);
				mv.visitLabel(labelNumLtFalseBr);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(labelPostNumLt);
			}
			case LE -> {
				Label labelNumLeFalseBr = new Label();
				mv.visitJumpInsn(IF_ICMPGT, labelNumLeFalseBr);
				mv.visitInsn(ICONST_1);
				Label labelPostNumLe = new Label();
				mv.visitJumpInsn(GOTO, labelPostNumLe);
				mv.visitLabel(labelNumLeFalseBr);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(labelPostNumLe);
			}
			case GT -> {
				Label labelNumGtFalseBr = new Label();
				mv.visitJumpInsn(IF_ICMPLE, labelNumGtFalseBr);
				mv.visitInsn(ICONST_1);
				Label labelPostNumGt = new Label();
				mv.visitJumpInsn(GOTO, labelPostNumGt);
				mv.visitLabel(labelNumGtFalseBr);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(labelPostNumGt);
			}
			case GE -> {
				Label labelNumGeFalseBr = new Label();
				mv.visitJumpInsn(IF_ICMPLT, labelNumGeFalseBr);
				mv.visitInsn(ICONST_1);
				Label labelPostNumGe = new Label();
				mv.visitJumpInsn(GOTO, labelPostNumGe);
				mv.visitLabel(labelNumGeFalseBr);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(labelPostNumGe);
			}
			default -> {
				throw new IllegalStateException("code gen bug in visitExpressionBinary NUMBER");
			}
			}
			;
		}
		case BOOLEAN -> {
			expressionBinary.e0.visit(this, arg);
			expressionBinary.e1.visit(this, arg);
			switch (op) {
			case PLUS -> mv.visitInsn(IOR);
			case TIMES -> mv.visitInsn(IAND);
			case EQ -> {
				Label labelNumEqFalseBr = new Label();
				mv.visitJumpInsn(IF_ICMPNE, labelNumEqFalseBr);
				mv.visitInsn(ICONST_1);
				Label labelPostNumEq = new Label();
				mv.visitJumpInsn(GOTO, labelPostNumEq);
				mv.visitLabel(labelNumEqFalseBr);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(labelPostNumEq);
			}
			case NEQ -> {
				Label labelNumNeqFalseBr = new Label();
				mv.visitJumpInsn(IF_ICMPEQ, labelNumNeqFalseBr);
				mv.visitInsn(ICONST_1);
				Label labelPostNumNeq = new Label();
				mv.visitJumpInsn(GOTO, labelPostNumNeq);
				mv.visitLabel(labelNumNeqFalseBr);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(labelPostNumNeq);
			}
			case LT -> {
				Label labelNumLtFalseBr = new Label();
				mv.visitJumpInsn(IF_ICMPGE, labelNumLtFalseBr);
				mv.visitInsn(ICONST_1);
				Label labelPostNumLt = new Label();
				mv.visitJumpInsn(GOTO, labelPostNumLt);
				mv.visitLabel(labelNumLtFalseBr);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(labelPostNumLt);
			}
			case LE -> {
				Label labelNumLeFalseBr = new Label();
				mv.visitJumpInsn(IF_ICMPGT, labelNumLeFalseBr);
				mv.visitInsn(ICONST_1);
				Label labelPostNumLe = new Label();
				mv.visitJumpInsn(GOTO, labelPostNumLe);
				mv.visitLabel(labelNumLeFalseBr);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(labelPostNumLe);
			}
			case GT -> {
				Label labelNumGtFalseBr = new Label();
				mv.visitJumpInsn(IF_ICMPLE, labelNumGtFalseBr);
				mv.visitInsn(ICONST_1);
				Label labelPostNumGt = new Label();
				mv.visitJumpInsn(GOTO, labelPostNumGt);
				mv.visitLabel(labelNumGtFalseBr);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(labelPostNumGt);
			}
			case GE -> {
				Label labelNumGeFalseBr = new Label();
				mv.visitJumpInsn(IF_ICMPLT, labelNumGeFalseBr);
				mv.visitInsn(ICONST_1);
				Label labelPostNumGe = new Label();
				mv.visitJumpInsn(GOTO, labelPostNumGe);
				mv.visitLabel(labelNumGeFalseBr);
				mv.visitInsn(ICONST_0);
				mv.visitLabel(labelPostNumGe);
			}
			default -> {
				throw new IllegalStateException("code gen bug in visitExpressionBinary BOOLEAN");
			}
			}
			;
		}
		case STRING -> {
			expressionBinary.e0.visit(this, arg);
			expressionBinary.e1.visit(this, arg);
			switch (op) {
			case PLUS -> mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
			case EQ -> {
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
			}
			case NEQ -> {
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
                mv.visitInsn(ICONST_1);
                mv.visitInsn(IXOR);
			}
			case LE -> {
				mv.visitInsn(SWAP);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
			}
			case LT -> {
				mv.visitInsn(SWAP);
				mv.visitInsn(DUP2);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
				mv.visitInsn(ICONST_1);
				Label labelStrLtTrueBr = new Label();
				mv.visitJumpInsn(IF_ICMPEQ, labelStrLtTrueBr);
				mv.visitInsn(POP2);
				mv.visitInsn(ICONST_0);
				
				Label labelPostStrLt = new Label();
				mv.visitJumpInsn(GOTO, labelPostStrLt);

				mv.visitLabel(labelStrLtTrueBr);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
				mv.visitInsn(INEG);
				mv.visitInsn(ICONST_1);
				mv.visitInsn(IADD);
				mv.visitLabel(labelPostStrLt);
			}
			case GE -> {
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false);
			}
			case GT -> {
				mv.visitInsn(DUP2);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "endsWith", "(Ljava/lang/String;)Z", false);
				mv.visitInsn(ICONST_1);
				Label labelStrGtTrueBr = new Label();
				mv.visitJumpInsn(IF_ICMPEQ, labelStrGtTrueBr);
				mv.visitInsn(POP2);
				mv.visitInsn(ICONST_0);
				
				Label labelPostStrGt = new Label();
				mv.visitJumpInsn(GOTO, labelPostStrGt);

				mv.visitLabel(labelStrGtTrueBr);
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
				mv.visitInsn(INEG);
				mv.visitInsn(ICONST_1);
				mv.visitInsn(IADD);
				mv.visitLabel(labelPostStrGt);
			}
			default -> {
				throw new IllegalStateException("code gen bug in visitExpressionBinary STRING");
			}
			}
			;
		}
		default -> {
			throw new IllegalStateException("code gen bug in visitExpressionBinary");
		}
		}
		return null;
	}

	@Override
	public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor) arg;
		if (expressionIdent.getDec() instanceof ConstDec){
			ConstDec cd = (ConstDec) expressionIdent.getDec();
			if (cd.getType() == Type.NUMBER)
				mv.visitLdcInsn((Integer) cd.val);
			else if (cd.getType() == Type.STRING)
				mv.visitLdcInsn((String) cd.val); 
			else if (cd.getType() == Type.BOOLEAN)
				mv.visitInsn((Boolean) cd.val == false ? ICONST_0 : ICONST_1 ); 
			else {
				throw new UnsupportedOperationException("Not type");
			}
		}
		else if (expressionIdent.getDec() instanceof VarDec){
			//figure out nesting
			int at = (this.curr.dec == null ? 0 : this.curr.dec.getNest() + 1);
			int target = expressionIdent.getDec().getNest();
			int changes = at - target;
			VarDec vd = (VarDec) expressionIdent.getDec();
			if(changes == 0){
				if (vd.getType() == Type.NUMBER){
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, this.fullyQualifiedClassName + (this.curr.dec == null ? "" : this.curr.dec.getClassName()), String.valueOf(vd.ident.getText()), "I");
				}
				else if (vd.getType() == Type.BOOLEAN){
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, this.fullyQualifiedClassName + (this.curr.dec == null ? "" : this.curr.dec.getClassName()), String.valueOf(vd.ident.getText()), "Z");
				}
				else if (vd.getType() == Type.STRING){
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, this.fullyQualifiedClassName + (this.curr.dec == null ? "" : this.curr.dec.getClassName()), String.valueOf(vd.ident.getText()), "Ljava/lang/String;");
				}
				else {
					throw new UnsupportedOperationException("Not type var");
				} 
			}
			else{
				mv.visitVarInsn(ALOAD, 0);
				Node temp = this.curr;
				for(int i = changes - 1; i > 0; i--){
					mv.visitFieldInsn(GETFIELD, this.fullyQualifiedClassName + temp.dec.getClassName(), "this$" + String.valueOf(target + i), "L" + this.fullyQualifiedClassName + (temp.parent.dec == null ? "" : temp.parent.dec.getClassName()) + ";"); 
					temp = temp.parent;
				}
				mv.visitFieldInsn(GETFIELD, this.fullyQualifiedClassName + temp.dec.getClassName(), "this$" + String.valueOf(target), "L" + this.fullyQualifiedClassName + (temp.parent.dec == null ? "" : temp.parent.dec.getClassName()) + ";"); 

				if (vd.getType() == Type.NUMBER){
					mv.visitFieldInsn(GETFIELD, this.fullyQualifiedClassName + (temp.parent.dec == null ? "" : temp.parent.dec.getClassName()), String.valueOf(vd.ident.getText()), "I");
				}
				else if (vd.getType() == Type.BOOLEAN){
					mv.visitFieldInsn(GETFIELD, this.fullyQualifiedClassName + (temp.parent.dec == null ? "" : temp.parent.dec.getClassName()), String.valueOf(vd.ident.getText()), "Z");	
				}
				else if (vd.getType() == Type.STRING){
					mv.visitFieldInsn(GETFIELD, this.fullyQualifiedClassName + (temp.parent.dec == null ? "" : temp.parent.dec.getClassName()), String.valueOf(vd.ident.getText()), "Ljava/lang/String;");
				}
				else {
					throw new UnsupportedOperationException("Not type var");
				} 
			}
		}
		else{
			throw new UnsupportedOperationException("Not type proc?");
		}

		return null;
	}

	@Override
	public Object visitExpressionNumLit(ExpressionNumLit expressionNumLit, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor)arg;
		mv.visitLdcInsn(expressionNumLit.getFirstToken().getIntValue());
		return null;
	}

	@Override
	public Object visitExpressionStringLit(ExpressionStringLit expressionStringLit, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor)arg;
		mv.visitLdcInsn(expressionStringLit.getFirstToken().getStringValue());
		return null;
	}

	@Override
	public Object visitExpressionBooleanLit(ExpressionBooleanLit expressionBooleanLit, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor)arg;
		mv.visitLdcInsn(expressionBooleanLit.getFirstToken().getBooleanValue());
		return null;
	}

	@Override
	public Object visitProcedure(ProcDec procDec, Object arg) throws PLPException {
		if (this.prePass){
			procDec.setClassName((String) arg);
			procDec.block.visit(this, arg);
		}
		else{
			String currName = this.fullyQualifiedClassName + this.curr.dec.getClassName();
			String enc = this.fullyQualifiedClassName + (this.curr.parent.dec == null ? "" : this.curr.parent.dec.getClassName());
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES); 
			cw.visit(V18, ACC_SUPER, currName, null, "java/lang/Object", new String[] { "java/lang/Runnable" }); 

			cw.visitSource(this.sourceFileName, null); 
 
			cw.visitNestHost(this.fullyQualifiedClassName); 
			
			//inner class for children
			for(Node n: this.curr.children){
				cw.visitInnerClass(this.fullyQualifiedClassName + n.dec.getClassName(), this.fullyQualifiedClassName + this.curr.dec.getClassName(), n.cname, 0);
			}

			//follow up the tree
			innerClassUp(this.curr, cw);
			
			FieldVisitor fv = cw.visitField(ACC_FINAL | ACC_SYNTHETIC, "this$" + String.valueOf(procDec.getNest()), "L" + enc + ";", null, null); 
			fv.visitEnd(); 

			MethodVisitor mv = cw.visitMethod(0, "<init>", "(L" + enc + ";)V", null, null); 
			mv.visitCode(); 
			
			mv.visitVarInsn(ALOAD, 0); 
			mv.visitVarInsn(ALOAD, 1); 
			mv.visitFieldInsn(PUTFIELD, currName, "this$"  + String.valueOf(procDec.getNest()), "L" + enc + ";"); 
			mv.visitVarInsn(ALOAD, 0); 
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false); 
			mv.visitInsn(RETURN); 
			mv.visitMaxs(2, 2); 
			mv.visitEnd();

			procDec.block.visit(this, cw);

			cw.visitEnd();

			this.recs.add(new GenClass(currName, cw.toByteArray()));
		}
		return null;
	}

	@Override
	public Object visitConstDec(ConstDec constDec, Object arg) throws PLPException {
		return null;
	}

	@Override
	public Object visitStatementEmpty(StatementEmpty statementEmpty, Object arg) throws PLPException {
		return null;
	}

	@Override
	public Object visitIdent(Ident ident, Object arg) throws PLPException {
		MethodVisitor mv = (MethodVisitor)arg;
		int at = (this.curr.dec == null ? 0 : this.curr.dec.getNest() + 1);
		int target = ident.getDec().getNest();
		int changes = at - target;
		VarDec vd = (VarDec) ident.getDec();
		if(changes == 0){
			if (vd.getType() == Type.NUMBER){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitInsn(SWAP);
				mv.visitFieldInsn(PUTFIELD, this.fullyQualifiedClassName + (this.curr.dec == null ? "" : this.curr.dec.getClassName()), String.valueOf(vd.ident.getText()), "I");
			}
			else if (vd.getType() == Type.BOOLEAN){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitInsn(SWAP);
				mv.visitFieldInsn(PUTFIELD, this.fullyQualifiedClassName + (this.curr.dec == null ? "" : this.curr.dec.getClassName()), String.valueOf(vd.ident.getText()), "Z");
			}
			else if (vd.getType() == Type.STRING){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitInsn(SWAP);
				mv.visitFieldInsn(PUTFIELD, this.fullyQualifiedClassName + (this.curr.dec == null ? "" : this.curr.dec.getClassName()), String.valueOf(vd.ident.getText()), "Ljava/lang/String;");
			}
			else {
				throw new UnsupportedOperationException("Not type var");
			} 
		}
		else{
			mv.visitVarInsn(ALOAD, 0);
			Node temp = this.curr;
			for(int i = changes - 1; i > 0; i--){
				mv.visitFieldInsn(GETFIELD, this.fullyQualifiedClassName + temp.dec.getClassName(), "this$" + String.valueOf(target + i), "L" + this.fullyQualifiedClassName + (temp.parent.dec == null ? "" : temp.parent.dec.getClassName()) + ";"); 
				temp = temp.parent;
			}
			mv.visitFieldInsn(GETFIELD, this.fullyQualifiedClassName + temp.dec.getClassName(), "this$" + String.valueOf(target), "L" + this.fullyQualifiedClassName + (temp.parent.dec == null ? "" : temp.parent.dec.getClassName()) + ";"); 
			
			mv.visitInsn(SWAP);
			if (vd.getType() == Type.NUMBER){
				mv.visitFieldInsn(PUTFIELD, this.fullyQualifiedClassName + (temp.parent.dec == null ? "" : temp.parent.dec.getClassName()), String.valueOf(vd.ident.getText()), "I");
			}
			else if (vd.getType() == Type.BOOLEAN){
				mv.visitFieldInsn(PUTFIELD, this.fullyQualifiedClassName + (temp.parent.dec == null ? "" : temp.parent.dec.getClassName()), String.valueOf(vd.ident.getText()), "Z");	
			}
			else if (vd.getType() == Type.STRING){
				mv.visitFieldInsn(PUTFIELD, this.fullyQualifiedClassName + (temp.parent.dec == null ? "" : temp.parent.dec.getClassName()), String.valueOf(vd.ident.getText()), "Ljava/lang/String;");
			}
			else {
				throw new UnsupportedOperationException("Not type var");
			} 
		}

		return null;
	}

	private void preorderNestings(Node src, ClassWriter cw){
		
		if (src == null)
			return;
	
		/* first print data of node */
		if(src.cname.length() > 0){
			cw.visitNestMember(this.fullyQualifiedClassName + src.dec.getClassName());
			cw.visitInnerClass(this.fullyQualifiedClassName + src.dec.getClassName(), this.fullyQualifiedClassName + (src.parent.dec == null ? "" : src.parent.dec.getClassName()), src.cname, 0); 
		}
	
		/* then recur on children */
		for(Node n: src.children){
			preorderNestings(n, cw);
		}

	}

	private void innerClassUp(Node src, ClassWriter cw){
		cw.visitInnerClass(this.fullyQualifiedClassName + src.dec.getClassName(), this.fullyQualifiedClassName + (src.parent.dec == null ? "" : src.parent.dec.getClassName()), src.cname, 0);

		if(src.parent.parent != null){
			innerClassUp(src.parent, cw);
		}

	}

}
