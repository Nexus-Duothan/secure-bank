const SELECTED_ACCOUNT_KEY = 'sb_selected_account_id';
const DEFAULT_ACCOUNT_ID = 'acc-demo-primary';

export const accountSelection = {
  getSelectedAccountId: () => localStorage.getItem(SELECTED_ACCOUNT_KEY) ?? DEFAULT_ACCOUNT_ID,
  setSelectedAccountId: (accountId: string) => {
    localStorage.setItem(SELECTED_ACCOUNT_KEY, accountId);
  },
  clear: () => {
    localStorage.removeItem(SELECTED_ACCOUNT_KEY);
  },
};

export default accountSelection;
