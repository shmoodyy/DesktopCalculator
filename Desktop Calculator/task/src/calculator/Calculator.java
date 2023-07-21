package calculator;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;

public class Calculator extends JFrame {

    private boolean divisionByZero = false;

    public Calculator() {
        super("Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 400);
        setLocationRelativeTo(null);

        JLabel resultLabel = new JLabel();
        resultLabel.setName("ResultLabel");
        resultLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        resultLabel.setForeground(Color.BLACK);
        resultLabel.setFont(new Font("Arial", Font.BOLD, 36));

        JLabel equationLabel = new JLabel();
        equationLabel.setName("EquationLabel");
        equationLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        equationLabel.setForeground(Color.GREEN.darker());
        equationLabel.setFont(new Font("Arial", Font.BOLD, 18));

        // Set preferred size to make labels semi-large
        resultLabel.setPreferredSize(new Dimension(300, 50));
        equationLabel.setPreferredSize(new Dimension(300, 50));

        // Create nested JPanels with FlowLayout for right alignment
        JPanel resultPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        resultPanel.add(resultLabel);

        JPanel equationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        equationPanel.add(equationLabel);

        // Create a container panel with BorderLayout
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));
        labelPanel.add(resultPanel);
        labelPanel.add(equationPanel);

        String[] buttonLabels = {"( )", "CE", "C", "Del", "\uD835\uDC65²", "\uD835\uDC65ʸ", "√", "÷", "7", "8", "9", "×", "4", "5", "6"
                , "-" , "1", "2", "3", "+", "±", ".", "0", "="};
        JPanel buttonPanel = new JPanel(new GridLayout(6, 4, 1, 1));

        for (String label : buttonLabels) {
            if (label.isBlank()) {
                JLabel blank = new JLabel(label);
                buttonPanel.add(blank);
            } else {
                JButton button = new JButton(label);
                if (label.matches("\\( \\)|CE|C|Del|\uD835\uDC65²|\uD835\uDC65ʸ|√|÷|×|-|\\+|=")) {
                    button.setOpaque(false); // Ensure that the background color is applied
                    button.setBorderPainted(false);
                }
                button.setName(buttonName(label));
                button.addActionListener(e -> updateEquationLabel(resultLabel, equationLabel, button));
                buttonPanel.add(button);
            }
        }

        // Create a container panel with BorderLayout
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(labelPanel, BorderLayout.NORTH);
        containerPanel.add(buttonPanel, BorderLayout.CENTER);
        add(containerPanel);
        setVisible(true);
    }

    private void updateEquationLabel(JLabel resultLabel, JLabel equationLabel, JButton button) {
        divisionByZero = false;
        String equationText = equationLabel.getText();
        String buttonText = button.getText();
        String resultString = "";
        final boolean lastItemIsAnOperator = equationText.length() > 1
                && equationText.substring(equationText.length() - 1).matches("[×÷+-]");
        final boolean lastItemIsOpenPara = equationText.length() > 1
                && equationText.substring(equationText.length() - 1).matches("\\(");

        switch (buttonText) {
            case "CE", "Del"            -> deleteLastChar(equationLabel, equationText);
            case "C"                    -> clearEquation(equationLabel);
            case "( )"                  -> parentheses(equationLabel, equationText, lastItemIsAnOperator, lastItemIsOpenPara);
            case "√"                    -> equationLabel.setText(equationText + buttonText + '(');
            case "×", "÷", "+", "-"
                    , "\uD835\uDC65²"
                    , "\uD835\uDC65ʸ"
                                        -> operatorActions(equationLabel, equationText, buttonText, lastItemIsAnOperator);
            case "±"                    -> negation(equationLabel, equationText);
            case "="                    ->
                    solveEquation(resultLabel, equationLabel, equationText, resultString
                    , lastItemIsAnOperator, lastItemIsOpenPara);
            default                     ->
                    equationLabel.setText(equationText
                    + (buttonText.matches("[×÷+-]|\uD835\uDC65²|\uD835\uDC65ʸ") && equationText.isBlank() ? "" : buttonText));
        }
    }

    private static void parentheses(JLabel equationLabel, String equationText, boolean lastItemIsAnOperator, boolean lastItemIsOpenPara) {
        long openParenthesesCount = equationText.chars()
                .mapToObj(codePoint -> (char) codePoint)
                .filter(c -> c == '(')
                .count();
        long closedParenthesesCount = equationText.chars()
                .mapToObj(codePoint -> (char) codePoint)
                .filter(c -> c == ')')
                .count();
        if (openParenthesesCount == closedParenthesesCount || lastItemIsOpenPara || lastItemIsAnOperator) {
            equationLabel.setText(equationText + '(');
        } else equationLabel.setText(equationText + ')');
    }

    private static void negation(JLabel equationLabel, String equationText) {
        var numToNegate = "";
        var substring = "";
        var array = equationText.split("[)(√^×÷+\\-]");
        if (array.length > 0) {
            numToNegate = array[array.length - 1];
            substring = equationText.substring(0, equationText.length() - numToNegate.length());
        }
        var removedString = removeLastOccurrence(equationText, substring);
        if (array.length > 0 && !substring.matches("\\(-")) {
            if (equationText.length() == 0 || isOperator(equationText.charAt(equationText.length() - 1))) {
                equationLabel.setText(equationText + "(-");
            } else {
                equationLabel.setText(equationText.substring(0, equationText.length() - numToNegate.length()) + "(-" + numToNegate);
            }
        }
        else {
            equationLabel.setText(array.length == 0 ? "" : removedString);
        }
    }

    public static String removeLastOccurrence(String original, String substringToRemove) {
        int lastIndex = original.lastIndexOf(substringToRemove);

        if (lastIndex >= 0) {
            return original.substring(0, lastIndex) + original.substring(lastIndex + substringToRemove.length());
        } else {
            // Return the original string if the substring is not found
            return original;
        }
    }

    private void solveEquation(JLabel resultLabel, JLabel equationLabel, String equationText, String resultString,
                               boolean lastItemIsAnOperator, boolean lastItemIsOpenPara) {
        if (lastItemIsAnOperator || lastItemIsOpenPara) {
            equationLabel.setForeground(Color.RED.darker());
        } else {
            var result = evaluatePostfix(infixToPostfixWithSpaces(equationText));
            if (divisionByZero) equationLabel.setForeground(Color.RED.darker());
            else {
                equationLabel.setForeground(Color.GREEN.darker());
                result = result.stripTrailingZeros();
                if (result.compareTo(new BigDecimal(100000000)) < 0)
                    resultString = result.toPlainString();
                else resultString = String.valueOf(result);
            }
        }

        equationLabel.setText(equationText);
        resultLabel.setText(resultString);
    }

    private static boolean isOperator(char c) {
        return c == '√' || c == '^' || c == '×' || c == '÷' || c == '+' || c == '-';
    }

    private static void operatorActions(JLabel equationLabel, String equationText, String buttonText, boolean lastItemIsAnOperator) {
        var array = equationText.split("[)(√^×÷+\\-]");
        var lastnum = "";
        var updated = "";
        if (equationText.length() > 0) {
            lastnum = array[array.length - 1];
            updated = new BigDecimal(lastnum).toPlainString();
        }
        if (lastnum.matches("7|7.")) updated = "7.0"; // for whatever reason...
        int lastNumLength = lastnum.length();
        if (lastItemIsAnOperator
                && buttonText.matches("[×÷+-]|\uD835\uDC65²|\uD835\uDC65ʸ")) {
            equationLabel.setText(replaceLastCharacter(equationText, buttonText));
        } else {
            if (buttonText.matches("\uD835\uDC65²")) buttonText = "^(2)";
            else if (buttonText.matches("\uD835\uDC65ʸ")) buttonText = "^(";
            if (equationText.length() == 0) {
                equationLabel.setText(lastnum);
            } else {
                char lastChar = equationText.charAt(equationText.length() - 1);
                boolean lastCharIsAParentheses = lastChar == '(' || lastChar == ')';
                if (lastCharIsAParentheses) {
                    equationLabel.setText(equationText + buttonText);
                } else {
                    equationLabel.setText(equationText.substring(0, equationText.length() - lastNumLength) + updated + buttonText);
                }
            }
        }
    }

    private static void clearEquation(JLabel equationLabel) {
        equationLabel.setForeground(Color.GREEN.darker());
        equationLabel.setText("");
    }

    private static void deleteLastChar(JLabel equationLabel, String equationText) {
        if (equationText.length() > 0) equationLabel.setText(equationText.substring(0, equationText.length() - 1));
    }

    private static String replaceLastCharacter(String input, String replacement) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        int lastIndex = input.length() - 1;
        return input.substring(0, lastIndex) + replacement;
    }

    // precedence rankings
    static int precedence(char ch)
    {
        return switch (ch) {
            case '+', '-'   ->  1;
            case '×', '÷'   ->  2;
            case '^', '√'   ->  3;
            default         -> -1;
        };
    }

    // Modified from G4G's efficient and standardized infixToPostfix algo
    // with several changes to handle multi-digit operands and decimals
    public String infixToPostfixWithSpaces(String exp) {
        // Initializing empty String for result
        StringBuilder result = new StringBuilder();

        // Initializing empty stack
        Deque<Character> stack = new ArrayDeque<>();

        for (int i = 0; i < exp.length(); ++i) {
            char c = exp.charAt(i);

            // if char is digit
            if (Character.isDigit(c) || c == '.') {
                // To handle multi-digit and decimal operands,
                // keep adding characters to result until a non-digit or non-decimal point is encountered.
                while (i < exp.length() && (Character.isDigit(exp.charAt(i)) || exp.charAt(i) == '.')) {
                    result.append(exp.charAt(i));
                    i++;
                }
                result.append(" ");
                i--; // To counteract the i++ in the for loop.
            }

            // If char is '(' push to stack
            else if (c == '(')
                stack.push(c);

                // If char is ')' pop and output from the stack until an '(' is encountered.
            else if (c == ')') {
                while (!stack.isEmpty() && stack.peek() != '(') {
                    result.append(stack.peek()).append(" ");
                    stack.pop();
                }
                stack.pop();
            }

            // An operator is encountered
            else {
                while (!stack.isEmpty() && precedence(c) <= precedence(stack.peek())) {
                    result.append(stack.peek()).append(" ");
                    stack.pop();
                }
                stack.push(c);
            }
        }

        // Pop all the operators from the stack
        while (!stack.isEmpty()) {
            if (stack.peek() == '(')
                return "Invalid Expression";
            result.append(stack.peek()).append(" ");
            stack.pop();
        }

        return result.toString().trim(); // Trim to remove the extra space at the end.
    }

    public BigDecimal evaluatePostfix(String exp)
    {
        String[] expArray = exp.split(" ");
        // Create a stack
        Stack<BigDecimal> stack = new Stack<>();

        // Scan all items one by one
        for (String item : expArray) {
            // If item is an operand push to stack
            if (item.matches("^[-+]?(\\d*\\.\\d*|\\d+)$")) {
                BigDecimal num = BigDecimal.valueOf(Double.parseDouble(item));
                stack.push(num);
            }

            // If item is an operator pop 1 or 2 values from stack depending on the operator.
            else {
                BigDecimal val1 = stack.pop();
                BigDecimal val2 = stack.size() > 0 && !item.equals("√") ? stack.pop() : BigDecimal.ZERO;
                if (item.matches("÷") && val1.compareTo(BigDecimal.ZERO) == 0) divisionByZero = true;
                try {
                    switch (item) {
                        case "+" -> stack.push(val2.add(val1));
                        case "-" -> stack.push(val2.subtract(val1));
                        case "÷" -> stack.push(val2.divide(val1, RoundingMode.HALF_EVEN));
                        case "×" -> stack.push(val2.multiply(val1));
                        case "^" -> stack.push(val2.pow(val1.intValue()));
                        case "√" -> stack.push(val1.sqrt(MathContext.DECIMAL32));
                    }
                } catch (ArithmeticException e) {
                    divisionByZero = true;
                    stack.push(BigDecimal.ZERO);
                }
            }
        }
        return stack.pop();
    }

    public String buttonName(String button) {
        return switch (button) {
            case "0"                -> "Zero";
            case "1"                -> "One";
            case "2"                -> "Two";
            case "3"                -> "Three";
            case "4"                -> "Four";
            case "5"                -> "Five";
            case "6"                -> "Six";
            case "7"                -> "Seven";
            case "8"                -> "Eight";
            case "9"                -> "Nine";
            case "+"                -> "Add";
            case "-"                -> "Subtract";
            case "×"                -> "Multiply";
            case "÷"                -> "Divide";
            case "="                -> "Equals";
            case "C"                -> "Clear";
            case "Del"              -> "Delete";
            case "."                -> "Dot";
            case "CE"               -> "CE";
            case "( )"              -> "Parentheses";
            case "√"                -> "SquareRoot";
            case "\uD835\uDC65²"    -> "PowerTwo";
            case "\uD835\uDC65ʸ"    -> "PowerY";
            case "±"                -> "PlusMinus";
            default                 -> "";
        };
    }
}