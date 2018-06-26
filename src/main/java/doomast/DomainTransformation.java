package doomast;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.ast.tools.GeneralUtils;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.AbstractASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.Arrays;

@GroovyASTTransformation(phase = CompilePhase.INSTRUCTION_SELECTION)
public class DomainTransformation extends AbstractASTTransformation {

    @Override
    public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
        if (IsValid(sourceUnit)) {
            addInnerClass(sourceUnit);
            sourceUnit.getAST().getClasses().stream()
                    .forEach(classNode -> {
                        visit(classNode);
                        displayInterfaces(classNode);
                    });
        }
    }

    static private boolean IsValid(SourceUnit sourceUnit) {

        URI sourceURI = sourceUnit.getSource().getURI();

        if (!IsValidURIPath(sourceURI.getPath()))
            return false;

        return true;
    }

    static private boolean IsValidURIPath(String path) {
        if (path == null)
            return false;

        if (!path.endsWith(".groovy"))
            return false;

        if (!path.contains("domain/pr/Account"))
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
    static private void visit(ClassNode classNode) {

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
    static private Statement RecordReadStatement(ClassNode domainClassNode, VariableExpression recordVariableExpression, Expression idExpression) {

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
    static private IfStatement IfNotRecordStatement(ClassNode domainClassNode, Expression recordExpression, Expression idExpression) {

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
    static private ThrowStatement ThrowRecordNotFoundStatement(ClassNode domainClassNode, Expression idExpression) {

        ArgumentListExpression exceptionArguments = new ArgumentListExpression(
                new ConstantExpression(domainClassNode.getNameWithoutPackage()),
                new ConstantExpression("read"),
                idExpression
        );

        ConstructorCallExpression newRecordNotFoundException = new ConstructorCallExpression(
                ClassHelper.make("pr.exception.RecordNotFoundException"),
                exceptionArguments
        );

        return new ThrowStatement(newRecordNotFoundException);
    }

    /**
     * class DateRange implements Validateable {
     * <p>
     * // Properties
     * Date startDate = Date.valueOf(LocalDate.now())
     * Date endDate
     * <p>
     * static constraints = {
     * endDate:
     * NULLABLE
     * }
     * }
     *
     * @param sourceUnit
     */
    private void addInnerClass(SourceUnit sourceUnit) {
        ClassNode implementsValidateable = new ClassNode(
                "grails.validation.Validateable",
                (Modifier.ABSTRACT + Modifier.INTERFACE + Modifier.PUBLIC),
                null
        );
        InnerClassNode innerClassNode = new InnerClassNode(
                new ClassNode(java.lang.Object.class),
                "pr.Account$DateRange",
                Modifier.STATIC,
                new ClassNode(java.lang.Object.class),
                new ClassNode[]{implementsValidateable},
                null
        );

        ArgumentListExpression arguments = new ArgumentListExpression();
        arguments.addExpression(new ClassExpression(new ClassNode(java.time.LocalDate.class)));
        arguments.addExpression(new ConstantExpression("now"));

        MethodCallExpression methodCallExpression = new MethodCallExpression(
                new ClassExpression(new ClassNode(java.sql.Date.class)),
                new ConstantExpression("valueOf"),
                arguments
        );

        FieldNode fieldNode = new FieldNode(
                "startDate",
                Modifier.PRIVATE,
                new ClassNode(java.sql.Date.class),
                innerClassNode,
                methodCallExpression
        );

        PropertyNode propertyNode = new PropertyNode(fieldNode, Modifier.PUBLIC, null, null);

        innerClassNode.getProperties().add(propertyNode);

        sourceUnit.getAST().addClass(innerClassNode);

    }

    private void displayInterfaces(ClassNode classNode) {
        if (classNode.getName().contains("Gone")) {
            classNode.getProperties().stream().forEach(propertyNode ->
                    {

                        FieldNode fieldNode = propertyNode.getField();
                        System.out.println("fieldNode " + fieldNode.getName());
                        System.out.println("propertyNode " + propertyNode.getName());
                        System.out.println("fieldNode-modifiers " + fieldNode.getModifiers());
                        System.out.println("propertyNode-modifiers " + propertyNode.getModifiers());
                        System.out.println("propertyNode-getter-block " + propertyNode.getGetterBlock());
                        System.out.println("propertyNode-setter-block " + propertyNode.getSetterBlock());
                        System.out.println("propertyNode-setter-block " + propertyNode.getSetterBlock());
                    }
            );
        }
    }
}
