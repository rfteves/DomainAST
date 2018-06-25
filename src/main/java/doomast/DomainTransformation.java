package doomast;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.ast.tools.GeneralUtils;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import java.lang.reflect.Modifier;
import java.net.URI;

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class DomainTransformation extends AbstractASTTransformation {

    @Override
    public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
        if (IsValid(sourceUnit)) {
            sourceUnit.getAST().getClasses().stream()
                    .forEach(classNode -> visit(classNode));
        }
    }

    private boolean IsValid(SourceUnit sourceUnit) {

        URI sourceURI = sourceUnit.getSource().getURI();

        if (!IsValidURIPath(sourceURI.getPath()))
            return false;

        return true;
    }

    private boolean IsValidURIPath(String path) {
        if (path == null)
            return false;

        if (!path.endsWith(".groovy"))
            return false;

        if (!path.contains("domain/"))
            return false;

        System.out.println("path is " + path);
        return true;
    }

    /*
        static getById(Long id) {
            def record = Domain.read(id)
            if (!record) {
                throw new RecordNotFoundException("Domain", "read", id)
            }
            record
        }
    */
    private void visit(ClassNode classNode) {

        Parameter idParameter = new Parameter(
                ClassHelper.make(Long.class),
                "id"
        );

        VariableExpression idVariableExpression = new VariableExpression(idParameter);
        VariableExpression recordVariableExpression = new VariableExpression("record");

        BlockStatement methodBlockStatement = new BlockStatement();
        methodBlockStatement.addStatement(RecordReadStatement(classNode, recordVariableExpression, idVariableExpression));
        methodBlockStatement.addStatement(IfNotRecordStatement(classNode, recordVariableExpression, idVariableExpression));
        methodBlockStatement.addStatement(new ExpressionStatement(recordVariableExpression));



        classNode.addMethod(
                "getById",
                Modifier.STATIC,
                ClassHelper.OBJECT_TYPE,
                new Parameter[]{idParameter},
                new ClassNode[0],
                methodBlockStatement
        );
    }

    /*
        def record = Domain.read(id)
     */
    private Statement RecordReadStatement(ClassNode domainClassNode, VariableExpression recordVariableExpression, Expression idExpression) {

        MethodCallExpression domainRead = new MethodCallExpression(
                new ClassExpression(domainClassNode),
                "read",
                idExpression
        );

        DeclarationExpression recordDeclaration = new DeclarationExpression(
                recordVariableExpression,
                GeneralUtils.ASSIGN,
                domainRead);

        return new ExpressionStatement(recordDeclaration);
    }

    /*
        if (!record) {
            throw new RecordNotFoundException("Domain, "read", id)
        }
    */
    private IfStatement IfNotRecordStatement(ClassNode domainClassNode, Expression recordExpression, Expression idExpression) {

        BooleanExpression notRecord = new BooleanExpression(
                new NotExpression(recordExpression)
        );

        BlockStatement throwException = new BlockStatement();
        throwException.addStatement(ThrowRecordNotFoundStatement(domainClassNode, idExpression));

        return new IfStatement(
                notRecord,
                throwException,
                new BlockStatement()
        );
    }

    /*
        throw new RecordNotFoundException("Domain", "read", id)
     */
    private ThrowStatement ThrowRecordNotFoundStatement(ClassNode domainClassNode, Expression idExpression) {

        ArgumentListExpression exceptionArguments = new ArgumentListExpression(
                new ConstantExpression(domainClassNode.getNameWithoutPackage()),
                new ConstantExpression("read"),
                idExpression
        );

        ConstructorCallExpression newRecordNotFoundException = new ConstructorCallExpression(
                ClassHelper.make("com.gotkcups.exception.RecordNotFoundException"),
                exceptionArguments
        );

        return new ThrowStatement(newRecordNotFoundException);
    }
}
