const SELECTED_ACCOUNT_KEY = 'sb_selected_account_id';

export const accountSelection = {
  /**
   * The account the customer last looked at, or null when they have not picked one yet. There is
   * no default id: which accounts exist is decided by the backend, per signed-in customer.
   */
  getSelectedAccountId: (): string | null => localStorage.getItem(SELECTED_ACCOUNT_KEY),
  setSelectedAccountId: (accountId: string) => {
    localStorage.setItem(SELECTED_ACCOUNT_KEY, accountId);
  },
  clear: () => {
    localStorage.removeItem(SELECTED_ACCOUNT_KEY);
  },
};

export default accountSelection;
