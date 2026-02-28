# Intermittent Blank Screen Debugging

The application occasionally renders a blank screen when navigating to the landing page or company pages. This usually indicates a **React runtime crash** that halts the rendering process.

## Hypothesis
The issue is likely caused by the frontend attempting to access properties of a `null` or `undefined` object before the data has fully loaded or when the backend returns an unexpected response (e.g., a 500 error or a timeout).

The intermittency suggests:
- **Cold Starts**: Backend instances on Cloud Run spinning up might cause initial timeouts.
- **BigQuery Latency**: Occasional slow queries might exceed the expected frontend fetch window.
- **Cache Misses**: First-time data processing might return a slightly different structure or take longer than the app's threshold.

## Proposed Resolution Steps
- [ ] **Implement Error Boundaries**: Wrap the main page components in a React `ErrorBoundary` to show a user-friendly error state instead of a blank screen.
- [ ] **Defensive Data Access**: Ensure all component logic uses optional chaining (`?.`) and provides default values for nested data structures (e.g., `data?.companyDetails?.name || 'Loading...'`).
- [ ] **API Response Validation**: Add a validation layer in `api.ts` to verify the structure of the JSON response before passing it to the state.
- [ ] **Timeout Handling**: Explicitly handle fetch timeouts on the client side with a retry mechanism or a clear "Request Timed Out" UI.
- [ ] **Logging**: Implement a lightweight logging utility to capture the stack trace of these crashes for easier identification.

See this web console error:

React has detected a change in the order of Hooks called by CompanyProfilePage. This will lead to bugs and errors if not fixed. For more information, read the Rules of Hooks: https://react.dev/link/rules-of-hooks

   Previous render            Next render
   ------------------------------------------------------
1. useContext                 useContext
2. useContext                 useContext
3. useContext                 useContext
4. useContext                 useContext
5. useContext                 useContext
6. useContext                 useContext
7. useContext                 useContext
8. useRef                     useRef
9. useContext                 useContext
10. useLayoutEffect           useLayoutEffect
11. useCallback               useCallback
12. useContext                useContext
13. useState                  useState
14. useState                  useState
15. useState                  useState
16. useState                  useState
17. useState                  useState
18. useState                  useState
19. useCallback               useCallback
20. useEffect                 useEffect
21. useMemo                   useMemo
22. useMemo                   useMemo
23. undefined                 useMemo
   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

updateHookTypesDev @ react-dom_client.js?v=9816e117:5594Understand this error
react-dom_client.js?v=9816e117:5792 Uncaught Error: Rendered more hooks than during the previous render.
    at updateWorkInProgressHook (react-dom_client.js?v=9816e117:5792:19)
    at updateMemo (react-dom_client.js?v=9816e117:6540:20)
    at Object.useMemo (react-dom_client.js?v=9816e117:18969:20)
    at exports.useMemo (chunk-RY7GF66K.js?v=9816e117:947:36)
    at CompanyProfilePage (CompanyProfilePage.tsx:84:28)
    at Object.react_stack_bottom_frame (react-dom_client.js?v=9816e117:18509:20)
    at renderWithHooks (react-dom_client.js?v=9816e117:5654:24)
    at updateFunctionComponent (react-dom_client.js?v=9816e117:7475:21)
    at beginWork (react-dom_client.js?v=9816e117:8525:20)
    at runWithFiberInDEV (react-dom_client.js?v=9816e117:997:72)Understand this error
react-dom_client.js?v=9816e117:6966 An error occurred in the <CompanyProfilePage> component.

Consider adding an error boundary to your tree to customize error handling behavior.
Visit https://react.dev/link/error-boundaries to learn more about error boundaries.

Relevant line: const paginatedRoles = useMemo(() => {