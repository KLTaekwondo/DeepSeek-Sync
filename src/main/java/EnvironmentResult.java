import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class EnvironmentResult {
    private boolean CheckResult;
    private String commandPath = "deepseek-tui";
    private String message;

    public boolean isCheckResult() {
        return CheckResult;
    }

    public void setCheckResult(boolean checkResult) {
        CheckResult = checkResult;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCommandPath() {
        return commandPath;
    }

    public void setCommandPath(String commandPath) {
        this.commandPath = commandPath;
    }
}
