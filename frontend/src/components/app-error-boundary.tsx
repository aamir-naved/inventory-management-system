import { Component, type ErrorInfo, type PropsWithChildren, type ReactNode } from "react";

type Props = PropsWithChildren<{
  fallback?: ReactNode;
}>;

type State = {
  hasError: boolean;
};

export class AppErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("Uncaught UI error", error, info.componentStack);
  }

  private handleReload = () => {
    window.location.reload();
  };

  private handleGoHome = () => {
    window.location.assign("/");
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="error-boundary">
          <div className="error-boundary__panel panel">
            <p className="brand-kicker">Something went wrong</p>
            <h1>This screen crashed</h1>
            <p>
              Your work on other screens is safe. Reload to continue, or go back to the home
              screen.
            </p>
            <div className="error-boundary__actions">
              <button type="button" className="primary-button" onClick={this.handleReload}>
                Reload page
              </button>
              <button type="button" className="ghost-button" onClick={this.handleGoHome}>
                Go home
              </button>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
