import React, { useEffect, useMemo, useState } from 'react';
import ReactDOM from 'react-dom';
import {
	ComposedModal,
	ModalHeader,
	ModalBody,
	ModalFooter,
	Button,
	DataTable,
	Table,
	TableHead,
	TableRow,
	TableHeader,
	TableBody,
	TableCell,
	TableToolbar,
	TableToolbarContent,
	TableToolbarSearch,
	Tag,
	CodeSnippet,
	Tabs,
	TabList,
	Tab,
	TabPanels,
	TabPanel,
	InlineNotification,
	Pagination,
	Loading
} from '@carbon/react';
import { TrashCan, Renew, View, CheckmarkFilled, ErrorFilled, Close } from '@carbon/icons-react';
import { executionHistoryAPI } from '../utils/api';
import './ExecutionHistory.css';

const headers = [
	{ key: 'time', header: 'Time' },
	{ key: 'title', header: 'Action' },
	{ key: 'method', header: 'Method' },
	{ key: 'url', header: 'URL' },
	{ key: 'status', header: 'Status' },
	{ key: 'duration', header: 'Duration' },
	{ key: 'actions', header: 'Actions' }
];

function formatDate(iso) {
	try {
		if (!iso) return '-';
		if (typeof iso === 'number') {
			const ms = iso > 1e12 ? iso : iso * 1000; // handle seconds vs ms
			return new Date(ms).toLocaleString();
		}
		if (typeof iso === 'object') {
			const secs = iso.epochSecond ?? iso.seconds ?? iso._seconds;
			if (secs != null) return new Date(Number(secs) * 1000).toLocaleString();
			const nested = iso.$date || iso.date || iso.time || iso.value;
			if (nested) return formatDate(nested);
		}
		const d = new Date(iso);
		return isNaN(d.getTime()) ? String(iso) : d.toLocaleString();
	} catch {
		return typeof iso === 'string' ? iso : '-';
	}
}

function formatDuration(ms) {
	if (ms == null) return '-';
	const n = typeof ms === 'string' ? Number(ms) : ms;
	if (Number.isNaN(n)) return '-';
	return `${n} ms`;
}

function StatusTag({ status, code }) {
	if (status === 'success') return <Tag type="green"><CheckmarkFilled style={{ marginRight: 4 }} /> {code || 200}</Tag>;
	if (status === 'error') return <Tag type="red"><ErrorFilled style={{ marginRight: 4 }} /> {code || 'ERR'}</Tag>;
	return <Tag type="cool-gray">{status || '-'}</Tag>;
}

export default function ExecutionHistory({ open, onClose, username, onRerun }) {
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState(null);
	const [rows, setRows] = useState([]);
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(10);
	const [search, setSearch] = useState('');
	const [selected, setSelected] = useState(null);
	const [detailsOpen, setDetailsOpen] = useState(false);

	const load = async () => {
		if (!username) return;
		setLoading(true); setError(null);
		try {
			const list = await executionHistoryAPI.getUserExecutions(username);
			const mapped = (list || []).map((item, idx) => ({
				id: item.id || String(idx),
				raw: item,
				time: item.timestamp,
				title: item.actionTitle || 'Manual Request',
				method: item.method || '-',
				url: item.url || '-',
				status: item.status || '-',
				duration: item.durationMs != null ? `${item.durationMs} ms` : '-',
			}));
			setRows(mapped);
		} catch (e) {
			setError(e.message || 'Failed to load history');
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => { if (open) load(); }, [open]);

	const filteredRows = useMemo(() => {
		const term = (search || '').toLowerCase();
		const data = rows.filter(r => !term ||
			(r.title && r.title.toLowerCase().includes(term)) ||
			(r.url && r.url.toLowerCase().includes(term)) ||
			(r.method && r.method.toLowerCase().includes(term))
		);
		const start = (page - 1) * pageSize;
		return data.slice(start, start + pageSize);
	}, [rows, search, page, pageSize]);

	const openDetails = (row) => {
		// Accept either mapped table row (with .raw) or underlying raw object
		setSelected(row?.raw || row || null);
		setDetailsOpen(true);
	};

	const handleDelete = async (row) => {
		try { await executionHistoryAPI.deleteById(row.id); load(); } catch (e) { setError(e.message); }
	};

	const handleClearAll = async () => {
		if (!username) return;
		try { await executionHistoryAPI.deleteAllForUser(username); load(); } catch (e) { setError(e.message); }
	};

	if (!open) return null;

	const modal = (
		<ComposedModal open={open} onClose={onClose} size="lg">
			{/* Single header (Carbon supplies its own close icon) */}
			<ModalHeader label="History" title="Execution History" />
			<ModalBody hasForm>
				{error && (
					<div style={{ marginBottom: 12 }}>
						<InlineNotification kind="error" title="Error" subtitle={error} onCloseButtonClick={() => setError(null)} lowContrast />
					</div>
				)}
				{loading ? (
					<div style={{ display: 'flex', justifyContent: 'center', padding: '2rem' }}>
						<Loading small withOverlay={false} description="Loading" />
					</div>
				) : (
					<DataTable rows={filteredRows} headers={headers} isSortable>
						{({ rows, headers, getHeaderProps, getRowProps, getTableProps, getToolbarProps }) => (
							<>
								{/* Toolbar must be rendered outside of the <table> element to avoid invalid nesting */}
								<TableToolbar {...getToolbarProps()}>
									<TableToolbarContent>
										<TableToolbarSearch persistent onChange={(e) => { setSearch(e.target.value); setPage(1); }} />
										<Button kind="danger--ghost" size="sm" renderIcon={TrashCan} onClick={handleClearAll}>Clear all</Button>
									</TableToolbarContent>
								</TableToolbar>
								<Table {...getTableProps()} size="lg">
									<TableHead>
										<TableRow>
											{headers.map(h => {
												const headerProps = getHeaderProps({ header: h }) || {};
												const { key: _k, ...restHeaderProps } = headerProps; // remove key to avoid passing via spread
												return (
													<TableHeader key={h.key} {...restHeaderProps}>{h.header}</TableHeader>
												);
											})}
										</TableRow>
									</TableHead>
									<TableBody>
										{rows.map(r => {
											const rowProps = getRowProps({ row: r }) || {};
											const { key: _rk, ...restRowProps } = rowProps; // strip key from spread
											return (
												<TableRow key={r.id} {...restRowProps}>
													<TableCell>{formatDate(r.cells[0].value)}</TableCell>
													<TableCell>{r.cells[1].value}</TableCell>
													<TableCell>{r.cells[2].value}</TableCell>
													<TableCell title={r.cells[3].value} style={{ maxWidth: 320, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{r.cells[3].value}</TableCell>
													{(() => {
														// Resolve the original mapped row by id to access raw record fields
														const mapped = filteredRows.find(fr => fr.id === r.id);
														const record = mapped?.raw;
														return (
															<TableCell><StatusTag status={r.cells[4].value} code={record?.statusCode} /></TableCell>
														);
													})()}
													<TableCell>{r.cells[5].value}</TableCell>
													<TableCell>
															<div style={{ display: 'flex', gap: 8 }}>
																{(() => {
																	const mapped = filteredRows.find(fr => fr.id === r.id);
																	const record = mapped?.raw || null;
																	return (
																		<>
																			<Button kind="tertiary" size="sm" renderIcon={View} onClick={() => record && openDetails(record)}>Details</Button>
																		<Button
																			kind="ghost"
																			size="sm"
																			renderIcon={Renew}
																			onClick={(e) => { e.stopPropagation(); onRerun && record && onRerun(record); }}
																		>
																			Re-run
																		</Button>
																			<Button kind="danger--ghost" size="sm" renderIcon={TrashCan} onClick={() => mapped && handleDelete(mapped)}>Delete</Button>
																		</>
																	);
																})()}
															</div>
														</TableCell>
												</TableRow>
											);
										})}
									</TableBody>
								</Table>
							</>
						)}
					</DataTable>
				)}
				<div style={{ marginTop: 12 }}>
					<Pagination
						page={page}
						pageSize={pageSize}
						pageSizes={[10, 20, 50]}
						totalItems={rows.filter(r => !search || r.title?.toLowerCase().includes(search.toLowerCase()) || r.url?.toLowerCase().includes(search.toLowerCase())).length}
						onChange={({ page, pageSize }) => { setPage(page); setPageSize(pageSize); }}
					/>
				</div>


			</ModalBody>
			<ModalFooter>
				<Button kind="secondary" onClick={onClose}>Close</Button>
			</ModalFooter>
		</ComposedModal>
	);

	const detailModal = (detailsOpen && selected) ? (
		<ComposedModal open={detailsOpen} onClose={() => setDetailsOpen(false)} size="lg">
			<ModalHeader title={selected.actionTitle || 'Execution Details'} />
			<ModalBody className="exec-details-body">
				<Tabs className="exec-tabs">
					<TabList aria-label="Execution details">
						<Tab>Summary</Tab>
						<Tab>Request</Tab>
						<Tab>Response</Tab>
						{selected.errorMessage && <Tab>Error</Tab>}
					</TabList>
					<TabPanels>
						<TabPanel>
							<div className="exec-panel-scroll">
								<div className="exec-details-grid">
									<div><strong>User:</strong> {selected.username || '-'}</div>
									<div><strong>When:</strong> {formatDate(selected.timestamp)}</div>
									<div><strong>Source:</strong> {selected.source || '-'}</div>
									<div><strong>Status:</strong> <StatusTag status={selected.status || '-'} code={selected.statusCode} /></div>
									<div><strong>Duration:</strong> {formatDuration(selected.durationMs)}</div>
									<div><strong>Method:</strong> {selected.method || '-'}</div>
									<div><strong>URL:</strong> <span style={{ wordBreak: 'break-all' }}>{selected.url || '-'}</span></div>
								</div>
							</div>
						</TabPanel>
						<TabPanel>
							<div className="exec-panel-scroll" style={{ display: 'grid', gap: 12 }}>
								<div>
									<div className="exec-section-title">Headers</div>
									<ul className="exec-kv-list">
										{(selected.requestHeaders || []).map((h, idx) => (
											<li key={idx}><code>{h.key}</code>: {h.value}</li>
										))}
									</ul>
								</div>
								<div>
									<div className="exec-section-title">Params</div>
									<ul className="exec-kv-list">
										{(selected.requestParams || []).map((p, idx) => (
											<li key={idx}><code>{p.key}</code>: {p.value}</li>
										))}
									</ul>
								</div>
								{selected.requestBody && (
									<div>
										<div className="exec-section-title">Body</div>
										<CodeSnippet type="multi" className="exec-code">
											{selected.requestBody}
										</CodeSnippet>
									</div>
								)}
							</div>
						</TabPanel>
						<TabPanel>
							<div className="exec-panel-scroll" style={{ display: 'grid', gap: 12 }}>
								<div>
									<div className="exec-section-title">Headers</div>
									<ul className="exec-kv-list">
										{(selected.responseHeaders || []).map((h, idx) => (
											<li key={idx}><code>{h.key}</code>: {h.value}</li>
										))}
									</ul>
								</div>
								<div>
									<div className="exec-section-title">Body</div>
									<CodeSnippet type="multi" className="exec-code">
										{selected.responseBody || '[empty]'}
									</CodeSnippet>
								</div>
							</div>
						</TabPanel>
						{selected.errorMessage && (
							<TabPanel>
								<div className="exec-panel-scroll" style={{ display: 'grid', gap: 12 }}>
									<InlineNotification kind="error" title="Execution failed" subtitle={selected.errorMessage} lowContrast />
									{selected.stackTrace && (
										<CodeSnippet type="multi" className="exec-code">
											{selected.stackTrace}
										</CodeSnippet>
									)}
								</div>
							</TabPanel>
						)}
					</TabPanels>
				</Tabs>
			</ModalBody>
			<ModalFooter>
				<Button kind="secondary" onClick={() => setDetailsOpen(false)}>Close</Button>
				<Button kind="primary" renderIcon={Renew} onClick={(e) => { e.stopPropagation(); onRerun && onRerun(selected); setDetailsOpen(false); }}>Re-run</Button>
			</ModalFooter>
		</ComposedModal>
	) : null;

	return (
		<>
			{ReactDOM.createPortal(modal, document.body)}
			{detailModal && ReactDOM.createPortal(detailModal, document.body)}
		</>
	);
}
